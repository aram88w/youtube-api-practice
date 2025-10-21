package youtube.youtube_api_practice.admin;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.Video;

import java.util.List;

@Transactional
@Repository
@RequiredArgsConstructor
public class AdminRepository {

    private final EntityManager em;

    List<Channel> getChannelsExtended() {
        return em.createQuery("select c from Channel c where c.commentStatus = 'COMMENT_EXTENDED'", Channel.class)
                .getResultList();
    }

    List<Video> getVideosHighThumbnail() {
        return em.createQuery("select v from Video v where v.thumbnailUrl not like '%maxresdefault.jpg'", Video.class)
                .getResultList();
    }
}
