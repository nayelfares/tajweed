package com.superpowered.effect

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.android.synthetic.main.activity_preview.*
import java.io.File
import java.io.IOException
import java.text.FieldPosition


class Preview : AppCompatActivity() {
    lateinit var videoPlayer:ExoPlayer
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

        // Get the device's sample rate and buffer size to enable
        // low-latency Android audio output, if available.
        var samplerateString: String? = null
        var buffersizeString: String? = null
        val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager != null) {
            samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        }
        if (samplerateString == null) samplerateString = "48000"
        if (buffersizeString == null) buffersizeString = "480"
        val samplerate = samplerateString.toInt()
        val buffersize = buffersizeString.toInt()

        // Files under res/raw are not zipped, just copied into the APK.
        // Get the offset and length to know where our file is located.

        // Files under res/raw are not zipped, just copied into the APK.
        // Get the offset and length to know where our file is located.
        val fd = File(audioURL)

        System.loadLibrary("PlayerExample") // load native library

        NativeInit(samplerate, buffersize, cacheDir.absolutePath) // start audio engine

        if (fd.exists())
              OpenFileFromAPK(audioURL, 0, fd.length().toInt()) // open audio file from APK


        play.setOnClickListener {
            if (videoPlayer.playWhenReady){
                videoPlayer.playWhenReady =false
                videoPlayer.seekTo(0)
                seekTo(0.0)
            }else{
                videoPlayer.playWhenReady =true
                TogglePlayback()
            }
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource? {
        return ExtractorMediaSource.Factory(
                DefaultDataSourceFactory(this, "Exoplayer-local")).createMediaSource(uri)
    }


    // Functions implemented in the native library.
    private external fun NativeInit(samplerate: Int, buffersize: Int, tempPath: String)
    private external fun OpenFileFromAPK(path: String, offset: Int, length: Int)
    private external fun onUserInterfaceUpdate(): Boolean
    private external fun TogglePlayback()
    private external fun onForeground()
    private external fun onBackground()
    private external fun Cleanup()
    private external fun getCurrentPosition(): Double
    private external fun seekTo(position: Double)
}