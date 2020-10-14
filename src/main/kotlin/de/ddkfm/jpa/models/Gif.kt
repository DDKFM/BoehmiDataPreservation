package de.ddkfm.jpa.models

import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import javax.persistence.*

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@DynamicUpdate
data class Gif(
    @Column
    val posterUrl : String?,
    @Column
    var mediaUrl : String?,
    @ElementCollection(fetch = FetchType.LAZY)
    var keywords : MutableList<String>,
    @Column(length = 64)
    var hash : String,
    @Column
    var deleted : Boolean = false
) : AbstractPersistableEntity<String>() {
    @OneToMany(mappedBy = "gif")
    var tweets : List<Tweet> = emptyList()
}
