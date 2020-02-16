package ru.panfio.legacytester.testclasses;

import ru.panfio.legacytester.dependencies.soundcloud.Music;
import ru.panfio.legacytester.dependencies.soundcloud.PlayHistory;
import ru.panfio.legacytester.dependencies.soundcloud.TrackInfo;

import java.util.List;
import java.util.Map;

public class Data {
    public static final Map<String, TrackInfo> tracksInfo = ru.panfio.legacytester.util.JsonUtils.parse("{\"732251920\":{\"id\":\"732251920\",\"artist\":\"Vesky\",\"title\":\"Leaving\",\"url\":\"https://soundcloud.com/vskymusic/leaving\"},\"746114746\":{\"id\":\"746114746\",\"artist\":\"vibe.digital\",\"title\":\"Episode 062 - A Look Forward at 2020\",\"url\":\"https://soundcloud.com/vibe-digital/episode062\"},\"745949599\":{\"id\":\"745949599\",\"artist\":\"-Bucky-\",\"title\":\"Bucky - Night Racer\",\"url\":\"https://soundcloud.com/bucky-music/bucky-night-racer\"}}", new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<java.lang.String, ru.panfio.legacytester.dependencies.soundcloud.TrackInfo>>() {
    });
    public static final List<Music> expectedMusicList = ru.panfio.legacytester.util.JsonUtils.parse("[{\"id\":1579856369307,\"externalId\":\"732251920\",\"type\":\"SOUNDCLOUD\",\"artist\":\"Vesky\",\"title\":\"Leaving\",\"listenTime\":\"2020-01-24T08:59:29.307Z\",\"url\":\"https://soundcloud.com/vskymusic/leaving\"},{\"id\":1579856079704,\"externalId\":\"745949599\",\"type\":\"SOUNDCLOUD\",\"artist\":\"-Bucky-\",\"title\":\"Bucky - Night Racer\",\"listenTime\":\"2020-01-24T08:54:39.704Z\",\"url\":\"https://soundcloud.com/bucky-music/bucky-night-racer\"},{\"id\":1579855591640,\"externalId\":\"746114746\",\"type\":\"SOUNDCLOUD\",\"artist\":\"vibe.digital\",\"title\":\"Episode 062 - A Look Forward at 2020\",\"listenTime\":\"2020-01-24T08:46:31.640Z\",\"url\":\"https://soundcloud.com/vibe-digital/episode062\"}]", new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ru.panfio.legacytester.dependencies.soundcloud.Music>>() {
    });
    public static final List<PlayHistory> recentlyPlayed = ru.panfio.legacytester.util.JsonUtils.parse("[{\"id\":1579856369307,\"externalId\":\"732251920\",\"listenTime\":\"2020-01-24T08:59:29.307Z\"},{\"id\":1579856079704,\"externalId\":\"745949599\",\"listenTime\":\"2020-01-24T08:54:39.704Z\"},{\"id\":1579855591640,\"externalId\":\"746114746\",\"listenTime\":\"2020-01-24T08:46:31.640Z\"}]", new com.fasterxml.jackson.core.type.TypeReference<List<ru.panfio.legacytester.dependencies.soundcloud.PlayHistory>>() {
    });
}
