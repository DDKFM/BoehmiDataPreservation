package de.ddkfm.jpa.models

import lombok.Getter
import lombok.Setter
import org.hibernate.annotations.GenericGenerator

import javax.persistence.*
import java.io.Serializable

@Setter
@Getter
@MappedSuperclass
abstract class AbstractPersistableEntity<ID : String> : Serializable {

    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    val id: ID? = null

    @Version
    val version: Long? = null
}
