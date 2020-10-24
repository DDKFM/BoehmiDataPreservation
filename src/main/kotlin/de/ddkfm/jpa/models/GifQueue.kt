package de.ddkfm.jpa.models

import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
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
data class GifQueue(
    @ElementCollection
    var tweetIds : MutableList<Long>,
    @ElementCollection
    var keywords : MutableList<String>,
    @Column
    var accepted : Boolean = false,
    @Column
    var created : LocalDateTime
) : AbstractPersistableEntity<String>()
