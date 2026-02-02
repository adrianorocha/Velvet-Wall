package blu.macaw.velvetwall.utils

import android.text.format.DateUtils

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    return DateUtils.getRelativeTimeSpanString(
        this,
        now,
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

// Exemplo: Converte "blacklist" para texto amigável
fun String.toHumanReason(): String {
    return when(this) {
        "BLACKLIST" -> "Lista Negra"
        "NOT_CONTACT" -> "Fora da Agenda"
        "PRIVATE" -> "Privado"
        else -> "Desconhecido"
    }
}