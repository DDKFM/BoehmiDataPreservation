package de.ddkfm.configuration

data class FederationConfiguration (
    var id : String,
    var secret : String,
    var systems : List<FederationSystemConfiguration> = emptyList()
)
