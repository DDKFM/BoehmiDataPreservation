package de.ddkfm.jpa.models

import lombok.Getter
import lombok.Setter
import org.hibernate.annotations.GenericGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.Serializable
import java.time.LocalDateTime
import javax.persistence.*

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AbstractPersistableEntity<ID : String> : Serializable {
    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    var id: ID? = null
    @Version
    val version: Long? = null

    @Column(name = "created_date", nullable = false, updatable = false)
    @CreatedDate
    var createdDate: LocalDateTime = LocalDateTime.now()

    @Column(name = "modified_date")
    @LastModifiedDate
    var modifiedDate: LocalDateTime = LocalDateTime.now()
}
