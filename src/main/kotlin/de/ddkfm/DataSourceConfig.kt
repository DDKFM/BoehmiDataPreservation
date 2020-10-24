package de.ddkfm

import de.ddkfm.configuration.DataConfiguration
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DataSourceConfig {
    @Bean
    fun getDataSource() : DataSource {
        val config = DataConfiguration.config.db
        return with(config) {
            DataSourceBuilder.create()
                .url("jdbc:postgresql://$host:$port/$name")
                .username(username)
                .password(password)
                .build()
        }

    }
}
