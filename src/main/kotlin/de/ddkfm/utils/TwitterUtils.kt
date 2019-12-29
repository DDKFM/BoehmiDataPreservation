package de.ddkfm.utils

import de.ddkfm.Twitter
import kong.unirest.Unirest

fun String.download() : ByteArray {
    return Unirest.get(this).asBytes().body
}


fun Long.getTweetUrl() : String {
    return Twitter.doWithTwitter { this.showStatus(this@getTweetUrl).mediaEntities.map { it.expandedURL }.first() }
}

fun Long.getPosterUrl() : String {
    return Twitter.doWithTwitter { this.showStatus(this@getPosterUrl).mediaEntities.map { it.mediaURLHttps}.first() }
}

