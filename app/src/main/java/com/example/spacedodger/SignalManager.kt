package com.example.spacedodger

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object SignalManager {
    private var vibrator: Vibrator? = null
    private var soundPool: SoundPool? = null
    private var soundBoom: Int = 0
    private var soundCollect: Int = 0

    fun init(context: Context) {
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        soundBoom = soundPool!!.load(context, R.raw.boom, 1)
        soundCollect = soundPool!!.load(context, R.raw.collect, 1)
    }

    fun vibrate() {
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator?.vibrate(300)
        }
    }

    fun playCrash() {
        soundPool?.play(soundBoom, 1f, 1f, 0, 0, 1f)
    }

    fun playCollect() {
        soundPool?.play(soundCollect, 1f, 1f, 0, 0, 1f)
    }
}