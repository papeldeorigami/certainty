package br.unicamp.feec.ia369y.certainty

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import android.media.MediaRecorder.AudioSource
import android.os.Environment
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Blob
import java.io.*
import android.content.Intent
import kotlinx.android.synthetic.main.activity_setup.*
import kotlin.concurrent.thread
import org.json.JSONObject

/**
 * This code is inspired by the following references:
 * - https://github.com/googleglass/gdk-waveform-sample
 *
 */

class MainActivity : AppCompatActivity() {

    private var isRecording = false
    private var isPlaying = false
    private var recordingFilePath = "";

    private val logTag = "MainActivity"

    private var mBufferSize: Int = 0
    private var mAudioBuffer: ByteArray? = null

    private var mShouldContinue = true

    private var server = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val b = intent.extras
        if (b != null) {
            server = b.getString("server")
        }

        recordingFilePath = (Environment.getExternalStorageDirectory().path + "/" + Constants.AUDIO_RECORDING_FILE_NAME)
        decibelTextView.text = ""

        val mediaPlayer: MediaPlayer? = MediaPlayer.create(applicationContext, R.raw.question1)

        playQuestionButton.setOnClickListener {
            playQuestionButton.isEnabled = false
            if (isPlaying) {
                mediaPlayer?.pause()
                isPlaying = false
                Toast.makeText(this@MainActivity, "Stop playing", Toast.LENGTH_SHORT).show()
            } else {
                isPlaying = true
                mediaPlayer?.start()
                Toast.makeText(this@MainActivity, "Start playing", Toast.LENGTH_SHORT).show()
            }
            playQuestionButton.isEnabled = true
        }

        recordAnswerButton.setOnClickListener {
            if (isRecording) {
                recordAnswerButton.isEnabled = false
                stopRunning()
                isRecording = false
                Toast.makeText(this@MainActivity, "Stop recording", Toast.LENGTH_SHORT).show()
                recordAnswerButton.isEnabled = true
            } else {
                recordAnswerButton.isEnabled = false
                isRecording = true
                record();
                Toast.makeText(this@MainActivity, "Start recording", Toast.LENGTH_SHORT).show()
                recordAnswerButton.isEnabled = true
            }
        }

        checkButton.setOnClickListener {
            checkButton.isEnabled = false
            uploadRecordingFile();
        }
    }

    private fun uploadRecordingFile() {
        Fuel.upload(server + Constants.CLASSIFIER_ENDPOINT)
            //.header(mapOf("CONTENT-TYPE" to "audio/*"))
            .source { request, url ->
                File(recordingFilePath)
            }
            .name { Constants.AUDIO_RECORDING_FILE_NAME }
            .also { println(it) }
            .responseString { request, response, result ->
                if (response.statusCode == 201) {
                    val responseStr = result.get()
                    Log.i(logTag, responseStr)

                    val json = JSONObject(responseStr);
                    val level = json.getDouble("level");
                    val label= json.getString("label");

                    val intent = Intent(this, ResultActivity::class.java)

                    val b = Bundle()
                    b.putDouble("level", level)
                    b.putString("label", label)
                    intent.putExtras(b)

                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "Could not connect to server (" + response.statusCode + ")", Toast.LENGTH_SHORT).show()
                    Log.i(logTag, response.toString())
                    checkButton.isEnabled = true
                }
            }
    }

    fun record() {
        Log.d(logTag, "Start recording")

        // Update the text view on the main thread.
        checkButton.isEnabled = false

        thread() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            // Compute the minimum required audio buffer size and allocate the buffer.
            mBufferSize = AudioRecord.getMinBufferSize(
                Constants.SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                Constants.AUDIO_ENCODING
            )
            mAudioBuffer = ByteArray(mBufferSize / 2)

            var outputStream: BufferedOutputStream? = null
            try {
                outputStream = BufferedOutputStream(FileOutputStream(recordingFilePath))
            } catch (e: FileNotFoundException) {
                Log.e(logTag, "File not found for recording ", e)
            }

            val record = AudioRecord(
                AudioSource.MIC,
                Constants.SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                Constants.AUDIO_ENCODING,
                mBufferSize
            )
            record.startRecording()

            while (shouldContinue()) {
                val status = record.read(mAudioBuffer!!, 0, mBufferSize / 2)
                if (status == AudioRecord.ERROR_INVALID_OPERATION ||
                    status == AudioRecord.ERROR_BAD_VALUE
                ) {
                    Log.e(logTag, "Error reading audio data!")
                    break;
                }
                try {
                    outputStream!!.write(mAudioBuffer!!, 0, mAudioBuffer!!.size)
                } catch (e: Exception) {
                    Log.e(logTag, "Error saving recording ", e)
                    break;
                }
                //mWaveformView.updateAudioData(mAudioBuffer)
                updateDecibelLevel()
            }

            outputStream!!.close()
            record.stop()
            record.release()
            Log.d(logTag, "Stop recording")

            // Update the text view on the main thread.
            checkButton.post { checkButton.isEnabled = true }
        }
    }

    /**
     * Gets a value indicating whether the thread should continue running.
     *
     * @return true if the thread should continue running or false if it should stop
     */
    @Synchronized
    private fun shouldContinue(): Boolean {
        return mShouldContinue
    }

    /** Notifies the thread that it should stop running at the next opportunity.  */
    @Synchronized
    fun stopRunning() {
        mShouldContinue = false
    }

    /**
     * Computes the decibel level of the current sound buffer and updates the appropriate text
     * view.
     */
    private fun updateDecibelLevel() {
        // Compute the root-mean-squared of the sound buffer and then apply the formula for
        // computing the decibel level, 20 * log_10(rms). This is an uncalibrated calculation
        // that assumes no noise in the samples; with 16-bit recording, it can range from
        // -90 dB to 0 dB.
        var sum = 0.0

        for (rawSample in mAudioBuffer!!) {
            val sample = rawSample / 32768.0
            sum += sample * sample
        }

        val rms = Math.sqrt(sum / mAudioBuffer!!.size)
        val db = 20 * Math.log10(rms)

        // Update the text view on the main thread.
        decibelTextView.post { decibelTextView.text = String.format("%1\$.1f dB", db) }
    }
}
