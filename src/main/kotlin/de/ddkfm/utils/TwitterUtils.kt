package de.ddkfm.utils

import de.ddkfm.Twitter
import kong.unirest.Unirest
import twitter4j.Status

fun Status.download() : ByteArray? {
    val url = this.mediaEntities
        .map { it.videoVariants.map { it.url } }
        .flatten()
        .firstOrNull()
        ?: return null
    return Unirest.get(url).asBytes().body
}
