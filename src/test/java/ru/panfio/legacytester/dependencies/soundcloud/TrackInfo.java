package ru.panfio.legacytester.dependencies.soundcloud;

import lombok.*;

@Getter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TrackInfo {
    private String id;
    private String artist;
    private String title;
    private String url;

    @Override
    public String toString() {
        return "TrackInfo{" +
                "id='" + id + '\'' +
                ", artist='" + artist + '\'' +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
