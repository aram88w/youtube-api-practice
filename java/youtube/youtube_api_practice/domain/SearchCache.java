package youtube.youtube_api_practice.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.springframework.data.domain.Persistable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "search_cache")
@Getter
@Setter
@NoArgsConstructor
public class SearchCache implements Persistable<String> {

    @Id
    private String keyword;

    @Type(JsonType.class)
    @Column(name = "channel_ids", columnDefinition = "json")
    private List<String> channelIds; // JSON 문자열

    @Column(name = "last_searched_at")
    private LocalDateTime lastSearchedAt;


    @Transient
    private boolean isNew = true;

    @Override
    public String getId() {
        return keyword;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }




//    @Transient
//    private static final ObjectMapper objectMapper = new ObjectMapper();
//
//    // JSON -> Set<String> 변환
//    public Set<String> getChannelIds() {
//        try {
//            return objectMapper.readValue(channelIdsJson, new TypeReference<Set<String>>() {});
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to parse channel IDs JSON", e);
//        }
//    }
//
//    // Set<String> -> JSON 변환
//    public void setChannelIds(Set<String> channelIds) {
//        try {
//            this.channelIdsJson = objectMapper.writeValueAsString(channelIds);
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to write channel IDs JSON", e);
//        }
//    }

}