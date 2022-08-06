package com.dominikkorsa.discordintegration.api

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class UnsupportedVersionException(val pluginVersion: Version) :
    Exception("The installed version of Discord Integration is $pluginVersion, but at least 4.0.0 is required")
