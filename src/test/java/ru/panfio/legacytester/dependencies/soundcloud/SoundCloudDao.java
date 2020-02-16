package ru.panfio.legacytester.dependencies.soundcloud;

import java.util.List;
import java.util.Map;

public interface SoundCloudDao {
    Map<String, TrackInfo> tracksInfo();
    List<PlayHistory> recentlyPlayed();
    List<PlayHistory> stub(String str, int inte, List<PlayHistory> list);
}
