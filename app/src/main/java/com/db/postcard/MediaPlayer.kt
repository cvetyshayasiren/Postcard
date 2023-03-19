package com.db.postcard

import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer

object MyMediaPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying: Boolean = false

    fun start(audio: AssetFileDescriptor){
        if(isPlaying) {return}
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.isLooping
        }
        mediaPlayer?.setDataSource(audio.fileDescriptor, audio.startOffset, audio.length)
        mediaPlayer?.prepare()
        mediaPlayer?.start()
        isPlaying = true
    }

    fun stop(){
        if (mediaPlayer != null){
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        }
    }

    fun pause(){
        mediaPlayer?.pause()
    }

    fun play(){
        mediaPlayer?.start()
    }
}