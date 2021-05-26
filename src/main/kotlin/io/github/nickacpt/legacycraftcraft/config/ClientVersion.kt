package io.github.nickacpt.legacycraftcraft.config

enum class ClientVersion(val launcherId: String, val optifineUrl: String? = null) {
    ONE_FIVE_TWO("1.5.2", "https://www.dropbox.com/s/qj3ceu7qtvlw7ay/OptiFine_1.5.2_HD_U_D5%20%282%29.zip?dl=1"),
    ONE_EIGHT_NINE("1.8.9");

    override fun toString(): String {
        return launcherId
    }}