package de.ddkfm.jpa.models

import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.envers.Audited
import java.time.LocalDateTime
import javax.persistence.*

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@DynamicUpdate
@Audited
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
