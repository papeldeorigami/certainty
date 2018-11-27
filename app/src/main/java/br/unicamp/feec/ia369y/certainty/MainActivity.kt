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


/**
 * This code is inspired by the following references:
 * - https://github.com/googleglass/gdk-waveform-sample
 *
 */

class MainActivity : AppCompatActivity() {

    private var mRecordingThread: RecordingThread? = null
    //private val mDecibelFormat: String? = null

    private var isRecording = false
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        decibelTextView.text = ""

        mRecordingThread = RecordingThread()

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
                mRecordingThread!!.stopRunning()
                isRecording = false
                Toast.makeText(this@MainActivity, "Stop recording", Toast.LENGTH_SHORT).show()
                recordAnswerButton.isEnabled = true
            } else {
                recordAnswerButton.isEnabled = false
                isRecording = true
                mRecordingThread!!.start()
                Toast.makeText(this@MainActivity, "Start recording", Toast.LENGTH_SHORT).show()
                recordAnswerButton.isEnabled = true
            }
        }
    }

    /**
     * A background thread that receives audio from the microphone and sends it to the waveform
     * visualizing view.
     */
    private inner class RecordingThread : Thread() {

        private var mBufferSize: Int = 0
        private var mAudioBuffer: ByteArray? = null

        private var mShouldContinue = true

        private val logTag = "RecordingThread"

        private var recordingFilePath = "";

        override fun run() {
            Log.d(logTag, "Start recording")
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)

            // Compute the minimum required audio buffer size and allocate the buffer.
            mBufferSize = AudioRecord.getMinBufferSize(
                Constants.SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                Constants.AUDIO_ENCODING
            )
            mAudioBuffer = ByteArray(mBufferSize / 2)

            recordingFilePath = (Environment.getExternalStorageDirectory().path
                    + "/" + Constants.AUDIO_RECORDING_FILE_NAME)
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
                    return
                }
                try {
                    outputStream!!.write(mAudioBuffer!!, 0, mAudioBuffer!!.size)
                } catch (e: Exception) {
                    Log.e(logTag, "Error saving recording ", e)
                    return
                }
                //mWaveformView.updateAudioData(mAudioBuffer)
                updateDecibelLevel()
            }

            outputStream!!.close()
            record.stop()
            record.release()
            Log.d(logTag, "Stop recording")

            uploadRecordingFile();
        }

        private fun uploadRecordingFile() {
            val someObject = File(recordingFilePath)
            Fuel.upload(Constants.ANSWERS_URL)
                .header(mapOf("CONTENT-TYPE" to "audio/*"))
                .blob { request, url ->
                    Blob(Constants.AUDIO_RECORDING_FILE_NAME, someObject.length(), { someObject.inputStream() })
                }
                .also { println(it) }
                .responseString { request, response, result -> Log.i(logTag, response.toString()) }
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
}
