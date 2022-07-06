package com.dominikkorsa.discordintegration.update

import com.dominikkorsa.discordintegration.utils.tryCompare

data class Version constructor(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String?,
    val buildMetadata: String?,
) {
    companion object {
        private val regex = Regex("^v?(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-([^+]+))?(?:\\+(.+))?\$")

        fun parse(versionString: String): Version {
            val match = regex.find(versionString) ?: throw Exception("Cannot parse version `$versionString`")
            return Version(
                match.groups[1]!!.value.toInt(),
                match.groups[2]!!.value.toInt(),
                match.groups[3]?.value?.toInt() ?: 0,
                match.groups[4]?.value,
                match.groups[5]?.value
            )
        }
    }

    fun isNeverThan(other: Version) = major.tryCompare(other.major)
        ?: minor.tryCompare(other.minor)
        ?: patch.tryCompare(other.patch)
        ?: (preRelease == null && other.preRelease !== null)
}
