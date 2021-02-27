package com.superpowered.effect

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.*
import kotlinx.android.synthetic.main.activity_preview.*


class Preview : AppCompatActivity() {
    lateinit var videoPlayer:ExoPlayer
    lateinit var audioPlayer: ExoPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        // getting default bandwidth
        // getting default bandwidth
        val videoURL=intent.getStringExtra("video")
        videoPlayer = ExoPlayerFactory.newSimpleInstance(this,
                DefaultRenderersFactory(this),
                DefaultTrackSelector(), DefaultLoadControl())
        exoView.setPlayer(videoPlayer)

        val uri = Uri.parse(videoURL)
        val mediaSource = buildMediaSource(uri)!!
        videoPlayer.prepare(mediaSource, false, false)

        val audioURL=intent.getStringExtra("audio")
        audioPlayer = ExoPlayerFactory.newSimpleInstance(this,
                DefaultRenderersFactory(this),
                DefaultTrackSelector(), DefaultLoadControl())

        val audioUri = Uri.parse(audioURL)
        val audioMediaSource = buildMediaSource(audioUri)!!
        audioPlayer.prepare(audioMediaSource, false, false)

        play.setOnClickListener {
            if (videoPlayer.playWhenReady){
                videoPlayer.playWhenReady =false
                audioPlayer.playWhenReady = false
                videoPlayer.seekTo(0)
                audioPlayer.seekTo(0)
            }else{
                videoPlayer.playWhenReady =true
                audioPlayer.playWhenReady = true
            }
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource? {
        return ExtractorMediaSource.Factory(
                DefaultDataSourceFactory(this, "Exoplayer-local")).createMediaSource(uri)
    }
}