package br.unicamp.feec.ia369y.certainty

import android.media.AudioFormat

object Constants {
    // The sampling rate for the audio recorder.
    const val SAMPLING_RATE = 44100
    // The encoding format for the audio recorder.
    const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    // Temporary file name for storing the recording
    const val AUDIO_RECORDING_FILE_NAME = "recording.wav"
    const val DEFAULT_BACKEND_URL = "http://192.168.0.14:3000"
    const val CLASSIFIER_ENDPOINT = "/classify"
    const val STATS_ENDPOINT = "/swagger-stats/stats"
}