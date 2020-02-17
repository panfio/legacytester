package ru.panfio.legacytester;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.panfio.legacytester.constructor.ConstructorConfiguration;
import ru.panfio.legacytester.constructor.MockTestConstructor;
import ru.panfio.legacytester.dependencies.MessageBus;
import ru.panfio.legacytester.dependencies.soundcloud.SoundCloudDao;
import ru.panfio.legacytester.testclasses.ManualProxy;
import ru.panfio.legacytester.testclasses.QualifierPure;

import java.util.ArrayList;
import java.util.List;

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
        List<String> generatedTest = new ArrayList<>();
        ManualProxy service = new ManualProxy(messageBus, soundCloudDao);
        service.tester = new LegacyTester(ManualProxy.class)
                .testSupplier(MockTestConstructor::new)
                .testHandler(test -> generatedTest.add(test
                        .configuration(new ConstructorConfiguration()
                                .verbose(false)
                                .testMethodNameGenerator(method -> method.getName() + "Test777")
                                .assertionClass(Assertions.class))
                        .construct()));
        service.setTester();
        service.tester.test(service::process, null);
        Assertions.assertEquals(
                "    @Test\n" +
                        "    public void processTest777() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, java.lang.reflect.InvocationTargetException {\n" +
                        "        //Please create a test class manually if necessary\n" +
                        "        ManualProxy testClass = new ManualProxy(null,null);\n" +
                        "\n" +
                        "        MessageBus messageBus = Mockito.mock(MessageBus.class);\n" +
                        "        Field messageBusField = testClass.getClass().getDeclaredField(\"messageBus\");\n" +
                        "        messageBusField.setAccessible(true);\n" +
                        "        messageBusField.set(testClass, messageBus);\n" +
                        "        SoundCloudDao soundCloudDao = Mockito.mock(SoundCloudDao.class);\n" +
                        "        Field soundCloudDaoField = testClass.getClass().getDeclaredField(\"soundCloudDao\");\n" +
                        "        soundCloudDaoField.setAccessible(true);\n" +
                        "        soundCloudDaoField.set(testClass, soundCloudDao);\n" +
                        "        //Given\n" +
                        "\n" +
                        "        Map<String, TrackInfo> tracksInfo0ResultInvocation = JsonUtils.parse(\"{\\\"732251920\\\":{\\\"id\\\":\\\"732251920\\\",\\\"artist\\\":\\\"Vesky\\\",\\\"title\\\":\\\"Leaving\\\",\\\"url\\\":\\\"https://soundcloud.com/vskymusic/leaving\\\"},\\\"746114746\\\":{\\\"id\\\":\\\"746114746\\\",\\\"artist\\\":\\\"vibe.digital\\\",\\\"title\\\":\\\"Episode 062 - A Look Forward at 2020\\\",\\\"url\\\":\\\"https://soundcloud.com/vibe-digital/episode062\\\"},\\\"745949599\\\":{\\\"id\\\":\\\"745949599\\\",\\\"artist\\\":\\\"-Bucky-\\\",\\\"title\\\":\\\"Bucky - Night Racer\\\",\\\"url\\\":\\\"https://soundcloud.com/bucky-music/bucky-night-racer\\\"}}\", new TypeReference<Map<String, TrackInfo>>() {});\n" +
                        "        org.mockito.Mockito.when(soundCloudDao.tracksInfo()).thenReturn(tracksInfo0ResultInvocation);\n" +
                        "        List<PlayHistory> recentlyPlayed1ResultInvocation = JsonUtils.parse(\"[{\\\"id\\\":1579856369307,\\\"externalId\\\":\\\"732251920\\\",\\\"listenTime\\\":\\\"2020-01-24T08:59:29.307Z\\\"},{\\\"id\\\":1579856079704,\\\"externalId\\\":\\\"745949599\\\",\\\"listenTime\\\":\\\"2020-01-24T08:54:39.704Z\\\"},{\\\"id\\\":1579855591640,\\\"externalId\\\":\\\"746114746\\\",\\\"listenTime\\\":\\\"2020-01-24T08:46:31.640Z\\\"}]\", new TypeReference<List<PlayHistory>>() {});\n" +
                        "        org.mockito.Mockito.when(soundCloudDao.recentlyPlayed()).thenReturn(recentlyPlayed1ResultInvocation);\n" +
                        "\n" +
                        "        //When\n" +
                        "        testClass.process();\n" +
                        "\n" +
                        "        //Then\n" +
                        "        final Mockito<String> sendAllarg0Captor = Mockito.forClass(String.class);\n" +
                        "        final Mockito<ArrayList> sendAllarg1Captor = Mockito.forClass(ArrayList.class);\n" +
                        "        Mockito.verify(messageBus, Mockito.times(1)).sendAll(sendAllarg0Captor.capture(),sendAllarg1Captor.capture());\n" +
                        "        List<String> sendAllarg0Result = sendAllarg0Captor.getAllValues();\n" +
                        "        List<ArrayList> sendAllarg1Result = sendAllarg1Captor.getAllValues();\n" +
                        "        String sendAllarg0ExpectedResult = \"[music]\";\n" +
                        "        String sendAllarg1ExpectedResult = \"[[Music{id=1579856369307, externalId='732251920', type=SOUNDCLOUD, artist='Vesky', title='Leaving', listenTime=2020-01-24T08:59:29.307Z, url='https://soundcloud.com/vskymusic/leaving'}, Music{id=1579856079704, externalId='745949599', type=SOUNDCLOUD, artist='-Bucky-', title='Bucky - Night Racer', listenTime=2020-01-24T08:54:39.704Z, url='https://soundcloud.com/bucky-music/bucky-night-racer'}, Music{id=1579855591640, externalId='746114746', type=SOUNDCLOUD, artist='vibe.digital', title='Episode 062 - A Look Forward at 2020', listenTime=2020-01-24T08:46:31.640Z, url='https://soundcloud.com/vibe-digital/episode062'}]]\";\n" +
                        "        Assertions.assertEquals(sendAllarg0ExpectedResult, sendAllarg0Result.toString());\n" +
                        "        Assertions.assertEquals(sendAllarg1ExpectedResult, sendAllarg1Result.toString());\n" +
                        "\n" +
                        "    }", generatedTest.get(0));
    }

    @Test
    void throwsException() {
        ManualProxy service = new ManualProxy(messageBus, soundCloudDao);
        service.throwsException();
//        Assertions.assertThrows(IllegalArgumentException.class, () -> service.throwsException());
    }
}
