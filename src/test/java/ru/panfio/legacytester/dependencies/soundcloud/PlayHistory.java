package ru.panfio.legacytester.dependencies.soundcloud;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.panfio.legacytester.dependencies.IsoInstantDeserializer;
import ru.panfio.legacytester.dependencies.IsoInstantSerializer;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayHistory {
    private Long id;
    private String externalId;
    @JsonSerialize(using = IsoInstantSerializer.class)
    @JsonDeserialize(using = IsoInstantDeserializer.class)
    private Instant listenTime;

    @Override
    public String toString() {
        return "PlayHistory{" +
                "id=" + id +
                ", externalId='" + externalId + '\'' +
                ", listenTime=" + listenTime +
                '}';
    }
}
