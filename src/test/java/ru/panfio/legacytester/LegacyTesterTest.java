package ru.panfio.legacytester;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.panfio.legacytester.dependencies.MessageBus;
import ru.panfio.legacytester.dependencies.soundcloud.SoundCloudDao;
import ru.panfio.legacytester.testclasses.ManualProxy;
import ru.panfio.legacytester.testclasses.QualifierPure;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.panfio.legacytester.testclasses.Data.recentlyPlayed;
import static ru.panfio.legacytester.testclasses.Data.tracksInfo;

public class LegacyTesterTest {
    MessageBus messageBus;
    SoundCloudDao soundCloudDao;

    @BeforeEach
    public void setUp() {
        messageBus = mock(MessageBus.class);
        soundCloudDao = mock(SoundCloudDao.class);
        when(soundCloudDao.recentlyPlayed()).thenReturn(recentlyPlayed);
        when(soundCloudDao.tracksInfo()).thenReturn(tracksInfo);
    }

    @Test
    void qualifierTest() {
        QualifierPure service = new QualifierPure(messageBus, soundCloudDao);
        service.process();
    }

    @Test
    void manualProxyConfiguration() {
        ManualProxy service = new ManualProxy(messageBus, soundCloudDao);
        service.process();
    }

    @Test
    void throwsException() {
        ManualProxy service = new ManualProxy(messageBus, soundCloudDao);
        service.throwsException();
//        Assertions.assertThrows(IllegalArgumentException.class, () -> service.throwsException());
    }
}
