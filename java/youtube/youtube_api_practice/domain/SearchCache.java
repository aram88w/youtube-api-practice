package youtube.youtube_api_practice.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "search_cache")
@Getter
@Setter
@NoArgsConstructor
public class SearchCache {

    @Id
    private String keyword;

    @Column(name = "channel_ids", columnDefinition = "TEXT")
    private String channelIdsJson; // JSON 문자열

    @Column(name = "last_searched_at")
    private LocalDateTime lastSearchedAt;


    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper();


    // JSON -> Set<String> 변환
    public Set<String> getChannelIds() {
        try {
            return objectMapper.readValue(channelIdsJson, new TypeReference<Set<String>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse channel IDs JSON", e);
        }
    }

    // Set<String> -> JSON 변환
    public void setChannelIds(Set<String> channelIds) {
        try {
            this.channelIdsJson = objectMapper.writeValueAsString(channelIds);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write channel IDs JSON", e);
        }
    }

    public void setLastSearchedAt() {
        this.lastSearchedAt = LocalDateTime.now();
    }
}
