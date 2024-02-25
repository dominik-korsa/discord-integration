package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.exception.ConfigNotSetException
import dev.dejvokep.boostedyaml.block.implementation.Section
import dev.dejvokep.boostedyaml.route.Route
import discord4j.rest.util.Color

fun Section.getTrimmedString(route: String, placeholder: String? = null) =
    getString(route)?.trimEnd()?.takeUnless { it == placeholder }

fun Section.getTrimmedString(route: Route, placeholder: String? = null) =
    getString(route)?.trimEnd()?.takeUnless { it == placeholder }

fun Section.requireTrimmedString(route: String, placeholder: String? = null) =
    getTrimmedString(route, placeholder) ?: throw ConfigNotSetException(routeAsString, route)

fun Section.requireTrimmedString(route: Route, placeholder: String? = null) =
    getTrimmedString(route, placeholder) ?: throw ConfigNotSetException(routeAsString, route.toString())

fun Section.requireBoolean(route: String) = getBoolean(route) ?: throw ConfigNotSetException(routeAsString, route)

fun Section.requireBoolean(route: Route) = getBoolean(route) ?: throw ConfigNotSetException(routeAsString, route.toString())

fun Section.requireInt(route: String) = getInt(route) ?: throw ConfigNotSetException(routeAsString, route)

fun Section.requireStringList(route: String, placeholder: String?): List<String> =
    getStringList(route)?.filter { it != placeholder } ?: throw ConfigNotSetException(routeAsString, route)

fun Section.requireStringList(route: Route, placeholder: String?): List<String> =
    getStringList(route)?.filter { it != placeholder } ?: throw ConfigNotSetException(routeAsString, route.toString())

fun Section.getColor(route: String): Color = Color.of(Integer.decode(requireTrimmedString(route)))
