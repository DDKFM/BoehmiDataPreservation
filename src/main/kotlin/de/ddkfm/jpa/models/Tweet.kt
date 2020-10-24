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
data class Tweet(
    @Column
    var tweetId : Long,
    @Column(length = 1024)
    var text : String,
    @Column
    var createdAt : LocalDateTime?,
    @ManyToOne
    @JoinColumn(name = "gif_id", nullable = false)
    var gif : Gif,
    @Column
    var deletedOnTwitter : Boolean = false,
    @OneToOne
    var user : Tweeter,
    @ElementCollection(fetch = FetchType.LAZY)
    var hashtags : MutableList<String>
) : AbstractPersistableEntity<String>()
