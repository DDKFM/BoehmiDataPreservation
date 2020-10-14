package de.ddkfm.twitter

import twitter4j.StallWarning
import twitter4j.Status
import twitter4j.StatusDeletionNotice
import twitter4j.StatusListener
import java.lang.Exception

class StreamListener(val streamFunction : Status.() -> Unit) : StatusListener {
    override fun onTrackLimitationNotice(p0: Int) {

    }

    override fun onStallWarning(p0: StallWarning?) {

    }

    override fun onException(p0: Exception?) {
        p0?.printStackTrace()
    }

    override fun onDeletionNotice(p0: StatusDeletionNotice?) {

    }

    override fun onStatus(p0: Status?) {
        p0?.streamFunction()
    }

    override fun onScrubGeo(p0: Long, p1: Long) {

    }
}
