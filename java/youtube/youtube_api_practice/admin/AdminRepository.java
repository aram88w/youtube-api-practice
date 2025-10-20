package youtube.youtube_api_practice.admin;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import youtube.youtube_api_practice.domain.Channel;

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
}
