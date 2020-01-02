package de.ddkfm.models

data class TwitterUser(
    val username : String,
    val name : String,
    val description : String,
    val follower : Int,
    val profileImage : String,
    val accountLink : String
)
