package youtube.youtube_api_practice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import youtube.youtube_api_practice.domain.SearchCache;

import java.util.List;

public interface SearchCacheRepository extends JpaRepository<SearchCache, String> {
}
