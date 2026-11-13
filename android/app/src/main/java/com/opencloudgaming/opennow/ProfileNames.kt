package com.opencloudgaming.opennow

internal const val MAX_PROFILE_NAME_LENGTH = 40

/** One owner for how every user-named input profile (physical input, keyboard layout) is cleaned. */
internal fun sanitizeProfileName(name: String): String =
    name.filterNot(Char::isISOControl).trim().take(MAX_PROFILE_NAME_LENGTH)
