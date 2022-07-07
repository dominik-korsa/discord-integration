package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.exception.ConfigNotSetException
import dev.dejvokep.boostedyaml.block.implementation.Section
import dev.dejvokep.boostedyaml.route.Route
import discord4j.rest.util.Color

fun Section.getTrimmedString(route: String) =
    getString(route)?.trimEnd() ?: throw ConfigNotSetException(routeAsString, route)

fun Section.getTrimmedString(route: Route) =
    getString(route)?.trimEnd() ?: throw ConfigNotSetException(routeAsString, route.toString())

fun Section.getBooleanSafe(route: String) = getBoolean(route) ?: throw ConfigNotSetException(routeAsString, route)

fun Section.getIntSafe(route: String) = getInt(route) ?: throw ConfigNotSetException(routeAsString, route)

fun Section.getStringListSafe(route: String): List<String> =
    getStringList(route) ?: throw ConfigNotSetException(routeAsString, route)

fun Section.getColor(route: String): Color = Color.of(Integer.decode(getTrimmedString(route)))
