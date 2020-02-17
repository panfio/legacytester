package ru.panfio.legacytester.testclasses;

import ru.panfio.legacytester.LegacyTester;
import ru.panfio.legacytester.Testee;
import ru.panfio.legacytester.constructor.ConstructorConfiguration;
import ru.panfio.legacytester.dependencies.*;
import ru.panfio.legacytester.dependencies.soundcloud.Music;
import ru.panfio.legacytester.dependencies.soundcloud.PlayHistory;
import ru.panfio.legacytester.dependencies.soundcloud.SoundCloudDao;
import ru.panfio.legacytester.dependencies.soundcloud.TrackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QualifierPure {
    MessageBus messageBus;
    SoundCloudDao soundCloudDao;

    public QualifierPure(MessageBus messageBus, SoundCloudDao soundCloudDao) {
        this.messageBus = messageBus;
        this.soundCloudDao = soundCloudDao;
    }

    public void process() {
        Map<String, TrackInfo> trackInfos = getTrackInfos();
        List<PlayHistory> listenedTracks = getListenedTracks();
        List<Music> collectedTracks = new LegacyTester(QualifierPure.class)
                .qualifier("collectTracks")
                .constructorConfiguration(new ConstructorConfiguration()
                        .bodySpace("  ")
                        .signatureSpace(" ")
                        .assertionClass("org.junit.jupiter.api.Assertions"))
                .test(() -> collectTracks(trackInfos, listenedTracks),
                        trackInfos, listenedTracks);
        sendMessages(collectedTracks);
    }

    @Testee(qualifier = "collectTracks")
    private List<Music> collectTracks(Map<String, TrackInfo> trackInfos,
                                      List<PlayHistory> listenedTracks) {
        List<Music> collectedTracks = new ArrayList<>();
        for (PlayHistory listenedTrack : listenedTracks) {
            final String externalId = listenedTrack.getExternalId();
            if (!trackInfos.containsKey(externalId)) {
                continue;
            }
            Music music = new LegacyTester(QualifierPure.class)
                    .qualifier("constructMusic")
                    .test(() -> constructMusic(listenedTrack, trackInfos.get(externalId)),
                            listenedTrack, trackInfos.get(externalId));
            collectedTracks.add(music);
        }
        return collectedTracks;
    }

    @Testee(qualifier = "constructMusic")
    private Music constructMusic(PlayHistory track, TrackInfo info) {
        return Music.builder()
                .id(track.getId())
                .listenTime(track.getListenTime())
                .externalId(info.getId())
                .artist(info.getArtist())
                .title(info.getTitle())
                .url(info.getUrl())
                .type(Music.Type.SOUNDCLOUD)
                .build();
    }

    private Map<String, TrackInfo> getTrackInfos() {
        return soundCloudDao.tracksInfo();
    }

    private List<PlayHistory> getListenedTracks() {
        return soundCloudDao.recentlyPlayed();
    }

    private void sendMessages(List<Music> musicList) {
        messageBus.sendAll("music", musicList);
    }
}
