package io.github.rajumark.sextant.desktop.data

/**
 * Plain-English summaries of Android permissions, keyed by permission name. Covers every platform permission the
 * sample APKs declare. Custom permissions (declared by an app itself) and health data types are described by rule.
 *
 * Keep the summaries short and factual. Add a row here when a new permission shows up in a real APK.
 */
object PermissionBank {
    data class Summary(
        /** Short category, shown as the label, e.g. "Location". */
        val group: String,
        /** What the permission means, in one plain sentence. */
        val meaning: String,
        /** When apps use it, in one short sentence. */
        val usage: String,
    )

    private fun s(
        group: String,
        meaning: String,
        usage: String,
    ) = Summary(group, meaning, usage)

    private val summaries: Map<String, Summary> =
        mapOf(
            "android.permission.INTERNET" to s("Network", "Connect to the internet.", "Load web pages, download data and talk to servers."),
            "android.permission.READ_EXTERNAL_STORAGE" to
                s(
                    "Storage",
                    "Read files on shared storage such as photos, downloads and documents.",
                    "Open or import files you choose. Android 13 and newer use media-specific permissions instead.",
                ),
            "android.permission.WRITE_EXTERNAL_STORAGE" to
                s(
                    "Storage",
                    "Create, change or delete files on shared storage.",
                    "Save exports or downloads. Mostly unused on Android 10 and newer (scoped storage).",
                ),
            "android.permission.ACCESS_NETWORK_STATE" to
                s(
                    "Network",
                    "See whether a network is connected and what kind it is.",
                    "Decide whether to sync or retry based on connectivity.",
                ),
            "android.permission.POST_NOTIFICATIONS" to
                s(
                    "Notifications",
                    "Show notifications to you.",
                    "Alerts, messages and reminders. You can turn them off in system settings.",
                ),
            "android.permission.FOREGROUND_SERVICE" to
                s(
                    "Background",
                    "Run a long task that you can see in the notification shade.",
                    "Keep music, a download or a call running while the app is not on screen.",
                ),
            "android.permission.WAKE_LOCK" to
                s("Background", "Keep the phone awake while a task runs.", "Finish a download or sync even when the screen is off."),
            "android.permission.VIBRATE" to s("System", "Make the phone vibrate.", "Haptic feedback and alert vibration."),
            "android.permission.RECEIVE_BOOT_COMPLETED" to
                s("Background", "Start work after the phone restarts.", "Restore reminders, alarms or background services after a reboot."),
            "android.permission.ACCESS_FINE_LOCATION" to
                s(
                    "Location",
                    "Know your precise location from GPS and networks.",
                    "Maps, navigation and nearby results. The app should use it only while you need it.",
                ),
            "android.permission.CAMERA" to s("Camera", "Use the camera.", "Take photos, scan codes or record video."),
            "android.permission.RECORD_AUDIO" to
                s("Microphone", "Record sound from the microphone.", "Voice notes, calls, recording and voice commands."),
            "android.permission.ACCESS_COARSE_LOCATION" to
                s("Location", "Know your approximate location, from networks.", "City-level weather or local content."),
            "android.permission.FOREGROUND_SERVICE_DATA_SYNC" to
                s(
                    "Background",
                    "Run a visible background task that syncs data.",
                    "Sync files or messages while you can see progress in the notification.",
                ),
            "android.permission.REQUEST_INSTALL_PACKAGES" to
                s("Apps", "Ask to install other apps.", "App stores and updaters install APK files. You still confirm each install."),
            "android.permission.USE_BIOMETRIC" to
                s("Security", "Use fingerprint or face unlock.", "Unlock the app or confirm a payment with your biometric."),
            "android.permission.USE_FINGERPRINT" to
                s("Security", "Use the fingerprint sensor (older Android).", "Same as biometric unlock, for Android 9 and older."),
            "android.permission.ACCESS_WIFI_STATE" to
                s("Wi-Fi", "See Wi-Fi networks and connection details.", "Show network information or check whether you are on Wi-Fi."),
            "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" to
                s("Background", "Run a visible task that plays media.", "Keep music or podcasts playing with the screen off."),
            "android.permission.MODIFY_AUDIO_SETTINGS" to
                s(
                    "Media",
                    "Change system audio settings.",
                    "Adjust volume or where audio plays.",
                ),
            "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" to
                s(
                    "Battery",
                    "Ask to be excluded from battery optimization.",
                    "Stop Android from pausing a sync or alarm app in the background.",
                ),
            "android.permission.BLUETOOTH" to
                s("Bluetooth", "Use Bluetooth (Android 11 and older).", "Connect to headphones, keyboards and other devices."),
            "android.permission.MANAGE_EXTERNAL_STORAGE" to
                s(
                    "Storage",
                    "Access all files on the device.",
                    "File managers and backup tools. Google Play allows it only for approved app types.",
                ),
            "android.permission.SYSTEM_ALERT_WINDOW" to
                s(
                    "Display",
                    "Draw over other apps.",
                    "Floating buttons, chat heads or overlays. It can be misused to trick taps, so check the reason.",
                ),
            "android.permission.READ_PHONE_STATE" to
                s(
                    "Phone",
                    "Read phone status such as SIM, call state and device identifiers.",
                    "Carrier and call-state checks. Device identifiers are sensitive.",
                ),
            "android.permission.BLUETOOTH_CONNECT" to
                s("Bluetooth", "Connect to paired Bluetooth devices.", "Use paired headphones, watches or accessories."),
            "android.permission.READ_CONTACTS" to s("Contacts", "Read your contacts.", "Find friends who use the app, or pick a contact."),
            "android.permission.READ_MEDIA_IMAGES" to
                s(
                    "Media",
                    "Read photos and images on the device.",
                    "Pick or share photos. Android 14 and newer can limit this to photos you choose.",
                ),
            "android.permission.READ_MEDIA_VIDEO" to s("Media", "Read videos on the device.", "Pick or play videos from your gallery."),
            "android.permission.QUERY_ALL_PACKAGES" to
                s(
                    "Apps",
                    "See every app installed on the device.",
                    "Launchers, security and app managers. Google Play restricts it to approved uses.",
                ),
            "android.permission.FOREGROUND_SERVICE_MICROPHONE" to
                s("Background", "Run a visible task that uses the microphone.", "Keep recording or a call active in the background."),
            "android.permission.FOREGROUND_SERVICE_SPECIAL_USE" to
                s(
                    "Background",
                    "Run a visible task for a special, declared purpose.",
                    "Uncommon background work that fits no other service type.",
                ),
            "android.permission.READ_MEDIA_AUDIO" to
                s(
                    "Media",
                    "Read audio files on the device.",
                    "Pick music or recordings from storage.",
                ),
            "android.permission.WRITE_CONTACTS" to s("Contacts", "Change or add contacts.", "Save a new contact or update one."),
            "android.permission.REQUEST_DELETE_PACKAGES" to
                s("Apps", "Ask to uninstall apps.", "App managers remove apps. You still confirm each one."),
            "android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION" to
                s("Apps", "Update apps without asking you.", "Silent updates. Usually only store or system apps can use it."),
            "com.android.launcher.permission.INSTALL_SHORTCUT" to
                s(
                    "Home screen",
                    "Add shortcuts to the home screen.",
                    "Create a shortcut to an item in the app. Newer Android no longer honours it.",
                ),
            "android.permission.READ_APP_BADGE" to s("Notifications", "Read app badge counts.", "Show unread counts on the launcher icon."),
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to
                s(
                    "Media",
                    "Read only the photos and videos you choose.",
                    "You pick which items the app can see (Android 14 partial access).",
                ),
            "android.permission.READ_SYNC_SETTINGS" to
                s("Sync", "Read which accounts sync and how.", "Check whether sync is on before syncing."),
            "android.permission.WRITE_SYNC_SETTINGS" to s("Sync", "Turn sync on or off for an account.", "Change how an account syncs."),
            "android.permission.MANAGE_OWN_CALLS" to
                s("Calls", "Manage the app's own phone calls.", "Calling apps place and end calls through the system."),
            "android.permission.AUTHENTICATE_ACCOUNTS" to
                s("Accounts", "Act as an account sign-in provider.", "Add accounts to the device when you sign in."),
            "android.permission.GET_ACCOUNTS" to
                s("Accounts", "See the accounts on the device.", "Pre-fill your email or let you pick an account."),
            "android.permission.USE_CREDENTIALS" to
                s("Accounts", "Ask for saved account credentials.", "Older sign-in to an account on the device. Deprecated."),
            "android.permission.CHANGE_WIFI_STATE" to
                s("Wi-Fi", "Turn Wi-Fi on or off, or change networks.", "Connect to a Wi-Fi network for a device or service."),
            "android.permission.READ_CALENDAR" to
                s("Calendar", "Read your calendar events.", "Show your schedule or add reminders around events."),
            "android.permission.WRITE_CALENDAR" to
                s(
                    "Calendar",
                    "Add, change or delete calendar events.",
                    "Create events from inside the app.",
                ),
            "android.permission.ACCESS_BACKGROUND_LOCATION" to
                s(
                    "Location",
                    "Use your location while the app is in the background.",
                    "Geofencing, trip tracking or reminders when you are elsewhere. Asked separately.",
                ),
            "android.permission.ACCESS_LOCAL_NETWORK" to
                s("Network", "Find and talk to devices on your local network.", "Discover printers, speakers or home devices."),
            "android.permission.FOREGROUND_SERVICE_LOCATION" to
                s("Background", "Run a visible task that uses location.", "Live location sharing or trip tracking you can see."),
            "android.permission.FOREGROUND_SERVICE_PHONE_CALL" to
                s("Background", "Run a visible ongoing call.", "Keep an active call running in the background."),
            "android.permission.ACCESS_NOTIFICATION_POLICY" to
                s("Notifications", "Change Do Not Disturb settings.", "Set quiet hours or let the app bypass them."),
            "android.permission.SCHEDULE_EXACT_ALARM" to
                s("Background", "Schedule alarms at an exact time.", "Alarms, clocks and reminders that must go off on time."),
            "android.permission.ENFORCE_UPDATE_OWNERSHIP" to
                s("Apps", "Mark itself as the owner of an app update.", "Store-level update ownership. Ordinary apps rarely need it."),
            "android.permission.PACKAGE_USAGE_STATS" to
                s(
                    "Apps",
                    "See which apps you use and for how long.",
                    "Digital wellbeing and usage trackers. You grant it in special settings.",
                ),
            "android.permission.BLUETOOTH_ADMIN" to
                s("Bluetooth", "Find and pair Bluetooth devices (Android 11 and older).", "Set up or manage Bluetooth connections."),
            "android.permission.FOREGROUND_SERVICE_REMOTE_MESSAGING" to
                s("Background", "Run a visible task for remote messages.", "Ongoing message or call delivery."),
            "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" to
                s("Background", "Run a visible task that captures the screen.", "Screen recording or casting while it runs."),
            "android.permission.USE_FULL_SCREEN_INTENT" to
                s(
                    "Notifications",
                    "Show full-screen alerts such as an incoming call.",
                    "Call and alarm screens that take over the display.",
                ),
            "android.permission.WRITE_SETTINGS" to
                s(
                    "System",
                    "Change system settings.",
                    "Adjust brightness, ringtone and similar options. You grant it in special settings.",
                ),
            "android.permission.DUMP" to
                s(
                    "System",
                    "Read internal system state for debugging.",
                    "Diagnostic tools. Normally granted only to the system or developers.",
                ),
            "android.permission.READ_LOGS" to
                s("System", "Read system log messages.", "Log viewers for debugging. Logs can contain sensitive data."),
            "android.permission.NFC" to s("Sensors", "Use NFC to read or write tags.", "Tap to pay, transit cards and tag scanning."),
            "android.permission.NEARBY_WIFI_DEVICES" to
                s("Wi-Fi", "Find nearby Wi-Fi devices without location access.", "Connect to local devices such as printers or TVs."),
            "android.permission.READ_SYNC_STATS" to s("Sync", "Read sync history and statistics.", "Show when accounts last synced."),
            "android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" to
                s("Background", "Download files without a notification.", "Quiet background downloads."),
            "android.permission.DISABLE_KEYGUARD" to
                s(
                    "Security",
                    "Turn off the lock screen.",
                    "Skip the lock screen while the app is active.",
                ),
            "android.permission.FOREGROUND_SERVICE_CAMERA" to
                s("Background", "Run a visible task that uses the camera.", "Keep recording or streaming video in the background."),
            "android.permission.BLUETOOTH_SCAN" to
                s(
                    "Bluetooth",
                    "Scan for nearby Bluetooth devices.",
                    "Find beacons, watches or accessories.",
                ),
            "android.permission.MANAGE_ACCOUNTS" to s("Accounts", "Add, remove or change accounts.", "Account managers."),
            "android.permission.CHANGE_NETWORK_STATE" to
                s(
                    "Network",
                    "Change network connectivity.",
                    "Switch networks or enable a connection.",
                ),
            "android.permission.GET_PACKAGE_SIZE" to
                s("Storage", "Measure how much storage an app uses.", "Storage tools show per-app sizes."),
            "android.permission.WRITE_SECURE_SETTINGS" to
                s("System", "Change protected system settings.", "Reserved for system and privileged tools."),
            "android.permission.CREDENTIAL_MANAGER_SET_ORIGIN" to
                s("Security", "Set the website that an app's passwords belong to.", "Password managers link saved credentials to sites."),
            "android.permission.FOREGROUND_SERVICE_SHORT_SERVICE" to
                s("Background", "Run a short visible task.", "Quick background work that you can see."),
            "android.permission.CALL_PHONE" to
                s("Phone", "Place a phone call without opening the dialer.", "Start a call directly from the app."),
            "com.android.alarm.permission.SET_ALARM" to
                s("Alarms", "Set alarms in the clock app.", "Alarm and timer apps add alarms to the clock."),
            "android.permission.ACCESS_MEDIA_LOCATION" to
                s("Location", "Read the location saved inside photos.", "Photo apps show where pictures were taken."),
            "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS" to
                s("Location", "Use extra location provider commands.", "Used together with location for faster GPS fixes."),
            "android.permission.TRANSMIT_IR" to s("Sensors", "Use the infrared blaster.", "Universal remote control apps."),
            "android.permission.BROADCAST_STICKY" to
                s(
                    "System",
                    "Send broadcasts that stay after they are sent.",
                    "Rarely needed. Deprecated.",
                ),
            "android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" to
                s("Background", "Run a visible task for a connected device.", "Keep a connection to a watch or accessory active."),
            "android.permission.INSTALL_SHORTCUT" to
                s("Home screen", "Add shortcuts to the home screen (older name).", "Same as the launcher shortcut permission."),
            "android.permission.RAISED_THREAD_PRIORITY" to
                s("System", "Raise the priority of app threads.", "Audio and game engines for smooth performance."),
            "android.permission.READ_CALL_STATE" to
                s("Phone", "Know whether a call is ringing, active or ended.", "Call apps react to the current call state."),
            "android.permission.READ_PHONE_NUMBERS" to
                s("Phone", "Read the phone numbers on the device.", "Show or verify the device's own number."),
            "android.permission.READ_PROFILE" to
                s("Contacts", "Read the owner's profile details.", "Read the name on the device. Older permission."),
            "com.android.vending.BILLING" to
                s("Billing", "Make in-app purchases through Google Play.", "Buying subscriptions or items inside the app."),
            "android.permission.ACCESS_HIDDEN_PROFILES" to
                s(
                    "System",
                    "Access hidden or work profiles.",
                    "Device-management and profile apps.",
                ),
            "android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY" to
                s("System", "Change the runtime permission policy.", "Device-management software."),
            "android.permission.BACKUP" to
                s("System", "Back up and restore app data.", "Backup tools. Android normally handles this itself."),
            "android.permission.CHANGE_COMPONENT_ENABLED_STATE" to
                s("System", "Turn parts of other apps on or off.", "App managers and launchers hide or show app parts."),
            "android.permission.CHANGE_OVERLAY_PACKAGES" to s("System", "Change overlay (theme) packages.", "Theme managers."),
            "android.permission.CLEAR_APP_CACHE" to
                s("Storage", "Clear the cache of other apps.", "Storage cleaners. Restricted to system tools."),
            "android.permission.CLEAR_APP_USER_DATA" to
                s("Storage", "Erase the data of other apps.", "Restricted to system and device-management tools."),
            "android.permission.DELETE_CACHE_FILES" to s("Storage", "Delete cache files.", "Cleanup tools. Limited to system use."),
            "android.permission.DELETE_PACKAGES" to s("Apps", "Uninstall apps.", "App managers. You still confirm."),
            "android.permission.DEVICE_POWER" to s("System", "Control device power.", "System power controls."),
            "android.permission.FORCE_STOP_PACKAGES" to s("Apps", "Force-stop other apps.", "Task killers. Restricted to system tools."),
            "android.permission.GET_APP_OPS_STATS" to s("System", "Read app permission usage history.", "Privacy dashboards. System use."),
            "android.permission.GET_RUNTIME_PERMISSIONS" to s("System", "Read which permissions apps have.", "Permission managers."),
            "android.permission.GET_TASKS" to
                s("Apps", "See which tasks are running. Deprecated.", "Older task managers. Use is now restricted."),
            "android.permission.GRANT_RUNTIME_PERMISSIONS" to
                s("System", "Grant permissions to other apps.", "System and device-management tools only."),
            "android.permission.INSTALL_PACKAGES" to s("Apps", "Install apps silently.", "System installers only."),
            "android.permission.INSTALL_TEST_ONLY_PACKAGE" to s("System", "Install test-only apps.", "Developer and test tools."),
            "android.permission.INTERACT_ACROSS_USERS" to s("System", "Act across user profiles.", "Multi-user device management."),
            "android.permission.INTERACT_ACROSS_USERS_FULL" to
                s("System", "Full access across user profiles.", "Device-management software."),
            "android.permission.INTERNAL_DELETE_CACHE_FILES" to s("Storage", "Delete cache files of other apps.", "System-only cleanup."),
            "android.permission.KILL_UID" to s("System", "Stop a process by user ID.", "System-only."),
            "android.permission.MANAGE_APPOPS" to
                s("System", "Change app permission (app ops) settings.", "System-only permission managers."),
            "android.permission.MANAGE_APP_OPS_MODES" to s("System", "Change app operation modes.", "System-only."),
            "android.permission.MANAGE_NETWORK_POLICY" to
                s("Network", "Manage network policy rules.", "Data-saver and network managers. System-level."),
            "android.permission.MANAGE_NOTIFICATION_LISTENERS" to
                s("Notifications", "Manage which apps can read notifications.", "System-level notification access."),
            "android.permission.MANAGE_SENSORS" to s("Sensors", "Manage sensor access.", "System-level sensor control."),
            "android.permission.MANAGE_USERS" to s("System", "Create and manage user accounts on the device.", "Device-management tools."),
            "android.permission.NETWORK_SETTINGS" to s("Network", "Change network settings.", "System networking tools."),
            "android.permission.REAL_GET_TASKS" to s("Apps", "See running tasks (system).", "System-only task viewer."),
            "android.permission.REVOKE_RUNTIME_PERMISSIONS" to s("System", "Remove permissions from other apps.", "System-only."),
            "android.permission.START_ANY_ACTIVITY" to s("System", "Start any app screen.", "System-only."),
            "android.permission.SUSPEND_APPS" to s("Apps", "Suspend other apps.", "Device management. System-only."),
            "android.permission.UPDATE_APP_OPS_STATS" to s("System", "Change app operation usage records.", "System-only."),
            "android.permission.UPDATE_DOMAIN_VERIFICATION_USER_SELECTION" to
                s("System", "Change which app opens which web links.", "System link-handling settings."),
            "com.android.permission.INSTALL_EXISTING_PACKAGES" to s("Apps", "Install apps that are already on the device.", "System-only."),
            "android.permission.CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS" to
                s("Security", "Read candidate saved passwords or passkeys.", "Password managers, with your approval."),
            "android.permission.TURN_SCREEN_ON" to s("Display", "Turn the screen on.", "Alarm and call apps wake the screen."),
            "android.permission.EYE_TRACKING_FINE" to
                s("Sensors", "Track eye movements.", "Eye-tracking headsets and accessibility features."),
            "android.permission.HAND_TRACKING" to s("Sensors", "Track hand movements.", "Headset and XR apps."),
            "android.permission.ACTIVITY_RECOGNITION" to
                s("Sensors", "Detect activity such as walking or driving.", "Step counters and fitness apps."),
            "android.permission.BLUETOOTH_ADVERTISE" to
                s(
                    "Bluetooth",
                    "Advertise as a Bluetooth device.",
                    "Let other devices find this phone.",
                ),
            "android.permission.CHANGE_WIFI_MULTICAST_STATE" to
                s("Wi-Fi", "Receive multicast packets on Wi-Fi.", "Find local devices such as speakers."),
            "android.permission.MANAGE_ASSISTANT_AUDIO" to s("Media", "Manage assistant audio.", "Voice assistant apps."),
            "android.permission.POST_PROMOTED_NOTIFICATIONS" to
                s("Notifications", "Post live, promoted notifications.", "Live updates such as deliveries or rides."),
            "android.permission.BIND_QUICK_SETTINGS_TILE" to
                s("System", "Show a tile in the quick settings panel.", "Quick-toggle tiles for the app."),
            "android.permission.BODY_SENSORS" to
                s("Sensors", "Read body sensors such as heart rate.", "Fitness and health apps read heart rate from a watch or sensor."),
            "android.permission.BROADCAST_CLOSE_SYSTEM_DIALOGS" to
                s("System", "Close system dialogs and menus.", "Rarely used. System-level control."),
            "android.permission.CHANGE_CONFIGURATION" to
                s("System", "Change the device configuration, such as language.", "Language and display setting tools."),
            "android.permission.FLASHLIGHT" to s("Camera", "Turn the camera flash on and off as a light.", "Torch and flashlight apps."),
            "android.permission.READ_CALL_LOG" to s("Phone", "Read your call history.", "Call-log backup or spam checks. Sensitive."),
            "android.permission.READ_PRIVILEGED_PHONE_STATE" to
                s("Phone", "Read privileged phone state and identifiers.", "Carrier and system phone apps only."),
            "android.permission.READ_SMS" to s("Messages", "Read your text messages.", "Read SMS for codes or messaging apps. Sensitive."),
            "android.permission.SEND_SMS" to s("Messages", "Send text messages.", "Send a text from the app, often after you confirm."),
            "android.permission.SET_WALLPAPER" to s("Display", "Change the wallpaper.", "Wallpaper apps set the background image."),
            "android.permission.STORAGE" to
                s(
                    "Storage",
                    "Access shared storage (very old Android).",
                    "Old name for the storage permission. Replaced on newer Android.",
                ),
            "com.android.providers.tv.permission.READ_EPG_DATA" to
                s("TV", "Read TV program guide data.", "Live TV apps read the channel guide."),
            "com.android.providers.tv.permission.WRITE_EPG_DATA" to
                s("TV", "Write TV program guide data.", "Live TV apps save the channel guide."),
        )

    /** The summary for [name], or null when there is none. Health data types are described by rule. */
    fun summary(name: String): Summary? = summaries[name] ?: if (name.startsWith(HEALTH_PREFIX)) healthSummary(name) else null

    private const val HEALTH_PREFIX = "android.permission.health."

    private fun healthSummary(name: String): Summary {
        val type =
            name
                .removePrefix(HEALTH_PREFIX)
                .removePrefix("READ_")
                .removePrefix("WRITE_")
                .lowercase()
                .replace('_', ' ')
        val access = if (name.contains("WRITE_")) "Write" else "Read"
        return Summary(
            group = "Health",
            meaning = "$access $type data from Health Connect.",
            usage = "Health and fitness apps, with your approval for each data type.",
        )
    }
}
