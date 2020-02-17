package ru.panfio.legacytester.testclasses;


import org.junit.jupiter.api.Assertions;
import ru.panfio.legacytester.FieldInvocationHandler;
import ru.panfio.legacytester.LegacyTester;
import ru.panfio.legacytester.Testee;
import ru.panfio.legacytester.constructor.ConstructorConfiguration;
import ru.panfio.legacytester.constructor.MockTestConstructor;
import ru.panfio.legacytester.dependencies.MessageBus;
import ru.panfio.legacytester.dependencies.soundcloud.Music;
import ru.panfio.legacytester.dependencies.soundcloud.PlayHistory;
import ru.panfio.legacytester.dependencies.soundcloud.SoundCloudDao;
import ru.panfio.legacytester.dependencies.soundcloud.TrackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManualProxy {
    public LegacyTester tester;
    MessageBus messageBus;
    SoundCloudDao soundCloudDao;

    public ManualProxy(MessageBus messageBus, SoundCloudDao soundCloudDao) {
        this.messageBus = messageBus;//tester.createFieldProxy(MessageBus.class, new FieldInvocationHandler(messageBus, "sendAll").setFieldName("messageBus"));
        this.soundCloudDao = soundCloudDao;//tester.createFieldProxy(SoundCloudDao.class, new FieldInvocationHandler(soundCloudDao).setFieldName("soundCloudDao"));
    }

    public void setTester() {
        this.messageBus = tester.createFieldProxy(MessageBus.class, new FieldInvocationHandler(messageBus, "sendAll").setFieldName("messageBus"));
        this.soundCloudDao = tester.createFieldProxy(SoundCloudDao.class, new FieldInvocationHandler(soundCloudDao).setFieldName("soundCloudDao"));
    }

    @Testee
    public void process() {
        tester.clearInvocations();
        tester.test(this::original, null);
    }

    private void original() {
        Map<String, TrackInfo> trackInfos = getTrackInfos();
        List<PlayHistory> listenedTracks = getListenedTracks();
        List<Music> collectedTracks = collectTracks(trackInfos, listenedTracks);
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
            Music music = constructMusic(listenedTrack, trackInfos.get(externalId));
            collectedTracks.add(music);
        }
        return collectedTracks;
    }

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


    public void throwsException() {
        Map<String, TrackInfo> trackInfos = getTrackInfos();

        trackInfos.values().forEach(trackInfo -> {
            new LegacyTester(ManualProxy.class)
                    .qualifier("throwsException")
                    .testSupplier(MockTestConstructor::new)
                    .testHandler(test -> System.out.println(test
                            .configuration(
                                    new ConstructorConfiguration().assertionClass(Assertions.class))
                            .construct()))
                    .test(() -> throwsException(null, trackInfo), null, trackInfo);
        });
    }

    @Testee(qualifier = "throwsException")
    private Music throwsException(PlayHistory track, TrackInfo info) {
        if (info.getArtist().length() < 7) {
            throw new NullPointerException("my message");
        }
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
