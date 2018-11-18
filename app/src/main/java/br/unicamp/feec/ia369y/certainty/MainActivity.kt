package br.unicamp.feec.ia369y.certainty

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import android.media.MediaRecorder.AudioSource
import android.util.Log


/**
 * This code is inspired by the following references:
 * - https://github.com/googleglass/gdk-waveform-sample
 *
 */

class MainActivity : AppCompatActivity() {

    // The sampling rate for the audio recorder.
    private val SAMPLING_RATE = 44100
    // The encoding format for the audio recorder.
    private val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

    private var mRecordingThread: RecordingThread? = null
    private var mBufferSize: Int = 0
    private var mAudioBuffer: ShortArray? = null
    private val mDecibelFormat: String? = null

    private var isRecording = false
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        decibelTextView.text = ""

        // Compute the minimum required audio buffer size and allocate the buffer.
        mBufferSize = AudioRecord.getMinBufferSize(
            SAMPLING_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AUDIO_ENCODING
        )
        mAudioBuffer = ShortArray(mBufferSize / 2)

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

        private var mShouldContinue = true

        override fun run() {
            Log.d("RecordingThread", "Start recording")
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)

            val record = AudioRecord(
                AudioSource.MIC,
                SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AUDIO_ENCODING,
                mBufferSize
            )
            record.startRecording()

            while (shouldContinue()) {
                record.read(mAudioBuffer!!, 0, mBufferSize / 2)
                //mWaveformView.updateAudioData(mAudioBuffer)
                updateDecibelLevel()
            }

            record.stop()
            record.release()
            Log.d("RecordingThread", "Stop recording")
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
