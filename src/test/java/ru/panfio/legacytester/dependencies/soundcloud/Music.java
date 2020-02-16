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
public class Music {
    public enum Type {SPOTIFY, SOUNDCLOUD}

    private Long id;
    private String externalId;
    private Type type;
    private String artist;
    private String title;
    @JsonSerialize(using = IsoInstantSerializer.class)
    @JsonDeserialize(using = IsoInstantDeserializer.class)
    private Instant listenTime;
    private String url;

    @Override
    public String toString() {
        return "Music{" +
                "id=" + id +
                ", externalId='" + externalId + '\'' +
                ", type=" + type +
                ", artist='" + artist + '\'' +
                ", title='" + title + '\'' +
                ", listenTime=" + listenTime +
                ", url='" + url + '\'' +
                '}';
    }
}
