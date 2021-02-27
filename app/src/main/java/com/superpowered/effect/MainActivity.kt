package com.superpowered.effect

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private var playing = false
    private var samplerate = 0
    private var buffersize = 0

    private var mIsRecording = false
    private var mVideoFileName=""
    lateinit var cameraHolder:CameraHolder
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createVideoFileName()
        cameraHolder=CameraHolder(this, textureView, mVideoFileName)
        // Checking permissions.
        val permissions = arrayOf(
                Manifest.permission.RECORD_AUDIO
        )
        for (s in permissions) {
            if (ContextCompat.checkSelfPermission(this, s) != PackageManager.PERMISSION_GRANTED) {
                // Some permissions are not granted, ask the user.
                ActivityCompat.requestPermissions(this, permissions, 0)
                return
            }
        }

        startRecording.setOnClickListener {
            if (mIsRecording ) {
                mIsRecording = false
                cameraHolder.mMediaRecorder.stop()
                cameraHolder.mMediaRecorder.reset()
                cameraHolder.startPreview()
                FFmpeg.execute("-y -i $mVideoFileName ${mVideoFileName.replace(".mp4","1.wav")}")
                FFmpeg.execute("-y -i $mVideoFileName -c copy -an ${mVideoFileName.replace(".mp4","1.mp4")}")
                val intent= Intent(this,Preview::class.java)
                intent.putExtra("video",mVideoFileName.replace(".mp4","1.mp4"))
                intent.putExtra("audio",mVideoFileName.replace(".mp4","1.wav"))
                startActivity(intent)
            } else {
                mIsRecording = true
                cameraHolder.startRecord()
                cameraHolder.mMediaRecorder.start()
            }
        }

        // Got all permissions, initialize.
        initialize()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // Called when the user answers to the permission dialogs.
        if (requestCode != 0 || grantResults.size < 1 || grantResults.size != permissions.size) return
        var hasAllPermissions = true
        for (grantResult in grantResults) if (grantResult != PackageManager.PERMISSION_GRANTED) {
            hasAllPermissions = false
            Toast.makeText(applicationContext, "Please allow all permissions for the app.", Toast.LENGTH_LONG).show()
        }
        if (hasAllPermissions) initialize()
    }

    private fun initialize() {
        // Get the device's sample rate and buffer size to enable
        // low-latency Android audio output, if available.
        var samplerateString: String? = null
        var buffersizeString: String? = null
        val audioManager = this.getSystemService(AUDIO_SERVICE) as AudioManager
        if (audioManager != null) {
            samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        }
        if (samplerateString == null) samplerateString = "48000"
        if (buffersizeString == null) buffersizeString = "480"
        samplerate = samplerateString.toInt()
        buffersize = buffersizeString.toInt()
        System.loadLibrary("EffectExample") // load native library
    }

    // Handle Start/Stop button toggle.
    fun ToggleStartStop(button: View?) {
        playing = if (playing) {
            StopAudio()
            false
        } else {
            StartAudio(samplerate, buffersize)
            true
        }
        val b = findViewById<Button>(R.id.startStop)
        b.text = if (playing) "Stop" else "Start"
    }

    public override fun onPause() {
        super.onPause()
        if (playing) onBackground()
        cameraHolder.closeCamera()
        cameraHolder.stopBackgroundThread()
    }

    public override fun onResume() {
        super.onResume()
        if (playing) onForeground()
        cameraHolder.startBackgroundThread()
        if (textureView!!.isAvailable) {
            cameraHolder.setupCamera(textureView!!.width, textureView!!.height)
            cameraHolder.connectCamera()
        } else {
            textureView!!.surfaceTextureListener = cameraHolder.mSurfaceTextureListener
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (playing) StopAudio()
    }

    override fun onWindowFocusChanged(hasFocas: Boolean) {
        super.onWindowFocusChanged(hasFocas)
        val decorView = window.decorView
        if (hasFocas) {
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }
    }

    @Throws(IOException::class)
    private fun createVideoFileName(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val prepend = "VIDEO_" + timestamp + "_"
        val videoFile = File(filesDir, "$prepend.mp4")
        mVideoFileName = videoFile.absolutePath
        return videoFile
    }


    // Functions implemented in the native library.
    private external fun StartAudio(samplerate: Int, buffersize: Int)
    private external fun StopAudio()
    private external fun onForeground()
    private external fun onBackground()
}