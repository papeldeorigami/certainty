package br.unicamp.feec.ia369y.certainty

import android.media.AudioFormat

object Constants {
    // The sampling rate for the audio recorder.
    const val SAMPLING_RATE = 44100
    // The encoding format for the audio recorder.
    const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    // Temporary file name for storing the recording
    const val AUDIO_RECORDING_FILE_NAME = "answer.pcm"
    const val BACKEND_URL = "https://jsonplaceholder.typicode.com"
    const val ANSWERS_ENDPOINT = "/posts"
    const val ANSWERS_URL = BACKEND_URL + ANSWERS_ENDPOINT
}