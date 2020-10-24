package de.ddkfm.configuration

data class Configuration (
    var twitter : TwitterConfiguration,
    var locations : LocationConfiguration = LocationConfiguration(),
    var federation :FederationConfiguration,
    var following : FollowingConfiguration,
    var db : DatabaseConfiguration = DatabaseConfiguration()
)
