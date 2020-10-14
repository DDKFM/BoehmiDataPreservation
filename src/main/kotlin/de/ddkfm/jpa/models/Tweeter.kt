package de.ddkfm.jpa.models

import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor
import org.hibernate.annotations.DynamicUpdate
import java.time.LocalDateTime
import javax.persistence.*

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@DynamicUpdate
data class Tweeter(
    @Column
    var userId : Long,
    @Column
    var name : String,
    @Column
    var screenName : String,
    @Column
    val profileImage : String
) : AbstractPersistableEntity<String>()
