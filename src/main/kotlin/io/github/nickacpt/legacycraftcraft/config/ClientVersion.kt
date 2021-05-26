package io.github.nickacpt.legacycraftcraft.config

enum class ClientVersion(val launcherId: String) {
    ONE_FIVE_TWO("1.5.2") {
        override fun getJarModUrlsToApply(): List<String> {
            return listOf(
                "https://maven.minecraftforge.net/net/minecraftforge/forge/1.5.2-7.8.1.738/forge-1.5.2-7.8.1.738-universal.zip",
                "https://www.dropbox.com/s/qj3ceu7qtvlw7ay/OptiFine_1.5.2_HD_U_D5%20%282%29.zip?dl=1")
        }
    },
    ONE_EIGHT_NINE("1.8.9");


    open fun getJarModUrlsToApply(): List<String> {
        return emptyList()
    }

    override fun toString(): String {
        return launcherId
    }
}