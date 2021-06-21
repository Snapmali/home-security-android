package com.snapkirin.homesecurity.util

import android.util.Patterns
import com.snapkirin.homesecurity.model.WifiAuthentication

object IsStringValid {

    const val VALID = 0
    const val BLANK = 1
    const val INVALID_EMAIL = 2
    const val INVALID_USERNAME = 3
    const val INVALID_PASSWORD = 4
    const val INVALID_VERIFICATION_CODE = 5
    const val INVALID_DEVICE_SCREEN_NAME = 6
    const val INVALID_BLUETOOTH_NAME = 7

    private val usernameRegex = Regex("^[a-zA-Z][\\w]{3,15}$")
    private val passwordRegex = Regex("^[\\w.!@#\$%^&*]{4,16}$")
    private val verificationCodeRegex = Regex("^[a-zA-Z0-9]{6}$")
    private val deviceScreenNameRegex = Regex("^.{1,10}$")
    private val bluetoothNameRegex = Regex("^HomeSecurity-.{1,10}$")
    private val wifiAuthenticationRegex = Regex("""\[([\w-]+)]""")

    private val wifiKeyManagement = listOf("WPA2-PSK", "WPA-PSK", "WPA2", "WPA")
    private val wifiCipher = listOf("WEP", "TKIP", "CCMP")

    fun isIdentifierValid(identifier: String) : Int {
        return when {
            identifier.isBlank() -> BLANK
            identifier.contains('@') -> isEmailValid(identifier)
            else -> isUsernameValid(identifier)
        }
    }

    fun isEmailValid(email: String) : Int {
        return when  {
            email.isBlank() -> BLANK
            Patterns.EMAIL_ADDRESS.matcher(email).matches() -> VALID
            else -> INVALID_EMAIL
        }
    }

    fun isUsernameValid(username: String) : Int {
        return when {
            username.isBlank() -> BLANK
            usernameRegex.matches(username) -> VALID
            else -> INVALID_USERNAME
        }
    }

    fun isPasswordValid(password: String) : Int {
        return when {
            password.isBlank() -> BLANK
            passwordRegex.matches(password) -> VALID
            else -> INVALID_PASSWORD
        }

    }

    fun isVerificationCodeValid(verificationCode: String): Int {
        return when {
            verificationCode.isBlank() -> BLANK
            verificationCodeRegex.matches(verificationCode) -> VALID
            else -> INVALID_VERIFICATION_CODE
        }
    }

    fun isDeviceScreenNameValid(screenName: String): Int {
        return when {
            screenName.isBlank() -> BLANK
            deviceScreenNameRegex.matches(screenName) -> VALID
            else -> INVALID_DEVICE_SCREEN_NAME
        }
    }

    fun isBluetoothNameValid(deviceName: String?): Int {
        return when {
            deviceName == null || deviceName.isBlank() -> BLANK
            bluetoothNameRegex.matches(deviceName) -> VALID
            else -> INVALID_BLUETOOTH_NAME
        }
    }

    fun getWifiAuthenticationType(capabilities: String) : WifiAuthentication {
        val capabilityResult = wifiAuthenticationRegex.findAll(capabilities)
        val keyManagement = mutableListOf<String>()
        var cipher = "NONE"
        for (result in capabilityResult) {
            for (akm in wifiKeyManagement)
                if (result.value.contains(akm)) {
                    keyManagement.add(akm)
                    break
                }
            for (c in wifiCipher)
                if (result.value.contains(c)) {
                    cipher = c
                    break
                }
        }
        if (keyManagement.isEmpty())
            keyManagement.add("NONE")
        return WifiAuthentication(keyManagement, cipher)
    }
}