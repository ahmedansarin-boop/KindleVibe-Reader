package com.kindlevibe.reader.reader

import org.json.JSONObject
import org.readium.r2.shared.publication.Locator

object LocatorCodec {

    fun encode(locator: Locator): String =
        locator.toJSON().toString()

    fun decode(json: String): Locator? = try {
        Locator.fromJSON(JSONObject(json))
    } catch (e: Exception) {
        null
    }
}
