package de.ddkfm.configuration

data class DatabaseConfiguration(
    var host : String = "localhost",
    var port : Int = 5432,
    var name : String = "boehmidatapreservation",
    var username : String = "boehmidatapreservation",
    var password : String = "boehmidatapreservation"
)
