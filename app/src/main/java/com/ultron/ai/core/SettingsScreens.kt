package com.ultron.ai.core

import android.provider.Settings

/**
 * Maps what the user says to a system settings screen.
 */
object SettingsScreens {

    enum class Screen(
        val action: String,
        val label: String,
        val keywords: List<String>
    ) {
        NFC(Settings.ACTION_NFC_SETTINGS, "NFC",
            listOf("nfc")),

        WIFI(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi",
            listOf("wifi", "wi-fi", "wireless")),

        BLUETOOTH(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth",
            listOf("bluetooth")),

        MOBILE_DATA(Settings.ACTION_DATA_ROAMING_SETTINGS, "Mobile Data",
            listOf("mobile data", "cellular", "cellular data", "roaming")),

        HOTSPOT("android.settings.TETHER_SETTINGS", "Hotspot",
            listOf("hotspot", "tethering", "personal hotspot")),

        AIRPLANE(Settings.ACTION_AIRPLANE_MODE_SETTINGS, "Airplane Mode",
            listOf("airplane mode", "flight mode", "airplane")),

        LOCATION(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Location",
            listOf("location", "gps")),

        BATTERY("android.settings.BATTERY_SAVER_SETTINGS", "Battery",
            listOf("battery", "power saver", "battery saver")),

        DISPLAY(Settings.ACTION_DISPLAY_SETTINGS, "Display",
            listOf("display", "brightness", "screen")),

        SOUND(Settings.ACTION_SOUND_SETTINGS, "Sound",
            listOf("sound", "volume", "ringtone")),

        APPS(Settings.ACTION_APPLICATION_SETTINGS, "App Settings",
            listOf("app settings", "applications", "apps", "installed apps")),

        STORAGE(Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "Storage",
            listOf("storage", "memory storage", "internal storage")),

        SECURITY(Settings.ACTION_SECURITY_SETTINGS, "Security",
            listOf("security", "screen lock", "fingerprint", "passcode")),

        ACCESSIBILITY(Settings.ACTION_ACCESSIBILITY_SETTINGS, "Accessibility",
            listOf("accessibility")),

        DEVELOPER(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "Developer Options",
            listOf("developer options", "developer settings", "usb debugging")),

        DATE_TIME(Settings.ACTION_DATE_SETTINGS, "Date & Time",
            listOf("date and time", "time zone", "clock settings")),

        LANGUAGE(Settings.ACTION_LOCALE_SETTINGS, "Language",
            listOf("language", "keyboard", "locale")),

        NOTIFICATIONS("android.settings.NOTIFICATION_SETTINGS", "Notifications",
            listOf("notifications", "notification settings")),

        VPN(Settings.ACTION_VPN_SETTINGS, "VPN",
            listOf("vpn")),

        ABOUT(Settings.ACTION_DEVICE_INFO_SETTINGS, "About Phone",
            listOf("about phone", "device info", "android version")),

        ROOT(Settings.ACTION_SETTINGS, "Settings",
            listOf("settings", "system settings", "device settings"));
    }

    fun match(phrase: String): Screen? {
        val text = SpokenText.normalise(phrase)
        return Screen.values()
            .filter { it != Screen.ROOT }
            .flatMap { screen -> screen.keywords.map { screen to it } }
            .sortedByDescending { it.second.length }
            .firstOrNull { (_, keyword) -> containsPhrase(text, keyword) }
            ?.first
            ?: Screen.ROOT.takeIf { Screen.ROOT.keywords.any { k -> containsPhrase(text, k) } }
    }

    private fun containsPhrase(normalisedText: String, keyword: String): Boolean {
        val key = SpokenText.normalise(keyword)
        if (key.contains(' ')) {
            return " $normalisedText ".contains(" $key ")
        }
        return normalisedText.split(' ').any { token -> sameWord(token, key) }
    }

    private fun sameWord(a: String, b: String): Boolean {
        if (a == b) return true
        val x = stem(a)
        val y = stem(b)
        if (x.length < MIN_STEM || y.length < MIN_STEM) return false
        return x.startsWith(y) || y.startsWith(x)
    }

    private fun stem(w: String): String = if (w.length > 5) w.dropLast(1) else w

    private const val MIN_STEM = 4
}
