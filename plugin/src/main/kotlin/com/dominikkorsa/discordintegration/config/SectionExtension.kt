package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.exception.ConfigNotSetException
import dev.dejvokep.boostedyaml.block.implementation.Section
import dev.dejvokep.boostedyaml.route.Route
import discord4j.rest.util.Color

fun Section.requireTrimmedString(route: String) =
    getString(route)?.trimEnd() ?: throw ConfigNotSetException(routeAsString, route)

fun Section.requireTrimmedString(route: Route) =
    getString(route)?.trimEnd() ?: throw ConfigNotSetException(routeAsString, route.toString())

fun Section.requireBoolean(route: String) = getBoolean(route) ?: throw ConfigNotSetException(routeAsString, route)

fun Section.requireString(route: String) = getString(route) ?: throw ConfigNotSetException(routeAsString, route)

fun Section.requireInt(route: String) = getInt(route) ?: throw ConfigNotSetException(routeAsString, route)

fun Section.requireStringList(route: String): List<String> =
    getStringList(route) ?: throw ConfigNotSetException(routeAsString, route)

fun Section.getColor(route: String): Color = Color.of(Integer.decode(requireTrimmedString(route)))
