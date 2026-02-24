package blu.macaw.velvetwall.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import blu.macaw.velvetwall.R

class VelvetSoundHelper(context: Context) {
    private val soundPool: SoundPool
    private var soundId: Int = 0

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attributes)
            .build()

        // Carrega o som da pasta res/raw
        soundId = soundPool.load(context, R.raw.success_sparkle, 1)
    }

    fun playSuccess() {
        if (soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}