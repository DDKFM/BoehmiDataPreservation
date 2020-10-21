package de.ddkfm.configuration

data class Configuration (
    var twitter : TwitterConfiguration,
    var locations : LocationConfiguration = LocationConfiguration(),
    var federation : List<FederationConfiguration> = emptyList(),
    var following : FollowingConfiguration
)
