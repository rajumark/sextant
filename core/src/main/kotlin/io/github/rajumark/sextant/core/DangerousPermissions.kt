package io.github.rajumark.sextant.core

/** Android platform permissions with protection level `dangerous` (runtime permissions), up to API 36. */
object DangerousPermissions {
    private val NAMES =
        setOf(
            "ACCEPT_HANDOVER",
            "ACCESS_BACKGROUND_LOCATION",
            "ACCESS_COARSE_LOCATION",
            "ACCESS_FINE_LOCATION",
            "ACCESS_MEDIA_LOCATION",
            "ACTIVITY_RECOGNITION",
            "ANSWER_PHONE_CALLS",
            "BLUETOOTH_ADVERTISE",
            "BLUETOOTH_CONNECT",
            "BLUETOOTH_SCAN",
            "BODY_SENSORS",
            "BODY_SENSORS_BACKGROUND",
            "CALL_PHONE",
            "CAMERA",
            "GET_ACCOUNTS",
            "NEARBY_WIFI_DEVICES",
            "POST_NOTIFICATIONS",
            "PROCESS_OUTGOING_CALLS",
            "RANGING",
            "READ_CALENDAR",
            "READ_CALL_LOG",
            "READ_CONTACTS",
            "READ_EXTERNAL_STORAGE",
            "READ_MEDIA_AUDIO",
            "READ_MEDIA_IMAGES",
            "READ_MEDIA_VIDEO",
            "READ_MEDIA_VISUAL_USER_SELECTED",
            "READ_PHONE_NUMBERS",
            "READ_PHONE_STATE",
            "READ_SMS",
            "RECEIVE_MMS",
            "RECEIVE_SMS",
            "RECEIVE_WAP_PUSH",
            "RECORD_AUDIO",
            "SEND_SMS",
            "USE_SIP",
            "UWB_RANGING",
            "WRITE_CALENDAR",
            "WRITE_CALL_LOG",
            "WRITE_CONTACTS",
            "WRITE_EXTERNAL_STORAGE",
        ).mapTo(HashSet()) { "android.permission.$it" } + "com.android.voicemail.permission.ADD_VOICEMAIL"

    fun isDangerous(name: String): Boolean = name in NAMES || name.startsWith("android.permission.health.")
}
