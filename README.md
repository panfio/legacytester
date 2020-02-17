
# Legacy Tester

Automatic test creation tool for complex methods with many dependencies. It leverages Java Reflection with JUnit and Mockito frameworks through capturing and comparing input and output data on the method and its dependencies.

Works well with primitive types, DTO / POJO objects that override the `toString()` method, and their collections.

## Installation

Clone and build LegacyTester:

```sh
git clone https://github.com/panfio/legacytester.git
cd legacytester
./mvnw install
```

Add a dependency into your project:

```xml
<dependency>
    <groupId>ru.panfio</groupId>
    <artifactId>legacytester</artifactId>
    <version>${version}</version>
</dependency>
```

## Demo

Suppose you have a ticket with some bug fix/improvements. You plunged into legacy code and found a place for change. But you doubt the solution to the problem because of the fragile and complex conditional logic inside.

```java
@Testee
public class MusicService implements Processing {
    
    @Autowired
    @Testee(affectedMethods = "sendAll")
    MessageBus messageBus;
    
    @Autowired
    SoundCloudDao soundCloudDao;
    
    /**
    * Dear programmer! When I wrote this code only I and God knew how it works.
    * Now only God knows.
    * If you have a courage to try optimizing or change something
    * plese increment the counter below as a warning to the next programmer.
    * TOTAL_HOURS_SPEND_HERE = 254
    */
    @Testee
    public List<Music> process(int integer, String str, boolean boot) {
        //Generate a mock with this call
        Map<String, TrackInfo> trackInfos = getTrackInfos();  //soundCloudDao.tracksInfo()
        List<PlayHistory> listenedTracks = getListenedTracks();  //soundCloudDao.recentlyPlayed()
        List<PlayHistory> what = soundCloudDao.stub("LEGACY TESTER MOCK TEST", 42, listenedTracks);
        ... //1000 lines of really complicated logic
        List<PlayHistory> isLove = soundCloudDao.stub("LEGACY TESTER SECOND", 24, null);
        List<Music> collectedTracks = collectTracks(trackInfos, listenedTracks);
        //Capture this event
        sendMessages(collectedTracks);  //messageBus.sendAll()
        return collectedTracks;
    }
    //...
}
```

Just add annotations and define a LegacyTesterBeanPostProcessor bean in the spring application configuration class.

```java
@SpringBootApplication
public class MySpringBootApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MySpringBootApplication.class, args);
    }

    @Bean
    public BeanPostProcessor legacyTesterBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                return new LegacyTesterBeanPostProcessor(bean, beanName).createProxy();
            }
        };
    }
}
```

Run the application and invoke a function with some real/test data.
This creates a test method that you can find in the application log. Just copy and paste it somewhere in the test class. Run and verify it. If you are lucky and the test passes then look on a code coverage and start refactoring.

```java
@Test
public void processTest() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    //Please create a test class manually if necessary
    ru.panfio.telescreen.handler.service.MusicService testClass = new ru.panfio.telescreen.handler.service.MusicService();

    ru.panfio.telescreen.handler.service.MessageBus messageBus = org.mockito.Mockito.mock(ru.panfio.telescreen.handler.service.MessageBus.class);
    Field messageBusField = testClass.getClass().getDeclaredField("messageBus");
    messageBusField.setAccessible(true);
    messageBusField.set(testClass, messageBus);

    ru.panfio.telescreen.handler.dao.SoundCloudDao soundCloudDao = org.mockito.Mockito.mock(ru.panfio.telescreen.handler.dao.SoundCloudDao.class);
    Field soundCloudDaoField = testClass.getClass().getDeclaredField("soundCloudDao");
    soundCloudDaoField.setAccessible(true);
    soundCloudDaoField.set(testClass, soundCloudDao);

    //Given
    //Original value: 123
    int integer = (int) ru.panfio.legacytester.util.SerializableUtils.serializeFromString("rO0ABXNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAB7");
    //Original value: 123
    java.lang.String str = (java.lang.String) ru.panfio.legacytester.util.SerializableUtils.serializeFromString("rO0ABXQAAzEyMw==");
    //Original value: true
    boolean boot = (boolean) ru.panfio.legacytester.util.SerializableUtils.serializeFromString("rO0ABXNyABFqYXZhLmxhbmcuQm9vbGVhbs0gcoDVnPruAgABWgAFdmFsdWV4cAE=");

    java.util.Map<java.lang.String, ru.panfio.telescreen.handler.model.soundcloud.TrackInfo> tracksInfo1ResultInvocation = ru.panfio.legacytester.util.JsonUtils.parse("{\"732251920\":...\"}}", new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<java.lang.String, ru.panfio.telescreen.handler.model.soundcloud.TrackInfo>>() {});
    org.mockito.Mockito.when(soundCloudDao.tracksInfo()).thenReturn(tracksInfo1ResultInvocation);
    java.util.List<ru.panfio.telescreen.handler.model.soundcloud.PlayHistory> recentlyPlayed2ResultInvocation = ru.panfio.legacytester.util.JsonUtils.parse("[{\"id\":1....640Z\"}]", new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ru.panfio.telescreen.handler.model.soundcloud.PlayHistory>>() {});
    org.mockito.Mockito.when(soundCloudDao.recentlyPlayed()).thenReturn(recentlyPlayed2ResultInvocation);
    java.util.List<ru.panfio.telescreen.handler.model.soundcloud.PlayHistory> stub3ResultInvocation = ru.panfio.legacytester.util.JsonUtils.parse("[{\"id\":1...Z\"}]", new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ru.panfio.telescreen.handler.model.soundcloud.PlayHistory>>() {});
    //Original value: LEGACY TESTER MOCK TEST
    java.lang.String str3PassedParameter = (java.lang.String) ru.panfio.legacytester.util.SerializableUtils.serializeFromString("rO0ABXQAF0xFR0FDWSBURVNURVIgTU9DSyBURVNU");
    //Original value: 42
    int inte3PassedParameter = (int) ru.panfio.legacytester.util.SerializableUtils.serializeFromString("rO0ABXNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAq");
    java.util.List<ru.panfio.telescreen.handler.model.soundcloud.PlayHistory> list3PassedParameter = ru.panfio.legacytester.util.JsonUtils.parse("[{\"id\":157985636930...8:46:31.640Z\"}]", new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ru.panfio.telescreen.handler.model.soundcloud.PlayHistory>>() {});
    org.mockito.Mockito.when(soundCloudDao.stub(str3PassedParameter,inte3PassedParameter,list3PassedParameter)).thenReturn(stub3ResultInvocation);
    java.util.List<ru.panfio.telescreen.handler.model.soundcloud.PlayHistory> stub4ResultInvocation = ru.panfio.legacytester.util.JsonUtils.parse("null", new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ru.panfio.telescreen.handler.model.soundcloud.PlayHistory>>() {});
    //Original value: LEGACY TESTER SECOND
    java.lang.String str4PassedParameter = (java.lang.String) ru.panfio.legacytester.util.SerializableUtils.serializeFromString("rO0ABXQAFExFR0FDWSBURVNURVIgU0VDT05E");
    //Original value: 24
    int inte4PassedParameter = (int) ru.panfio.legacytester.util.SerializableUtils.serializeFromString("rO0ABXNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAY");
    java.util.List<ru.panfio.telescreen.handler.model.soundcloud.PlayHistory> list4PassedParameter = ru.panfio.legacytester.util.JsonUtils.parse("null", new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ru.panfio.telescreen.handler.model.soundcloud.PlayHistory>>() {});
    org.mockito.Mockito.when(soundCloudDao.stub(str4PassedParameter,inte4PassedParameter,list4PassedParameter)).thenReturn(stub4ResultInvocation);

    //When
    java.util.List<ru.panfio.telescreen.handler.model.Music> result = testClass.process(integer,str,boot);

    //Then
    final org.mockito.ArgumentCaptor<java.lang.String> sendAlltopicCaptor = org.mockito.ArgumentCaptor.forClass(java.lang.String.class);
    final org.mockito.ArgumentCaptor<java.util.ArrayList> sendAllmessagesCaptor = org.mockito.ArgumentCaptor.forClass(java.util.ArrayList.class);
    org.mockito.Mockito.verify(messageBus, org.mockito.Mockito.times(1)).sendAll(sendAlltopicCaptor.capture(),sendAllmessagesCaptor.capture());
    List<java.lang.String> sendAlltopicResult = sendAlltopicCaptor.getAllValues();
    List<java.util.ArrayList> sendAllmessagesResult = sendAllmessagesCaptor.getAllValues();
    String sendAlltopicExpectedResult = "[music]";
    String sendAllmessagesExpectedResult = "[[Music{id=1579856369307, externalId='73225..../episode062'}]]";

    org.junit.Assert.assertEquals(sendAlltopicExpectedResult, sendAlltopicResult.toString());
    org.junit.Assert.assertEquals(sendAllmessagesExpectedResult, sendAllmessagesResult.toString());

    String expectedResult = "[Music...url='https://soundcloud.com/vibe-digital/episode062'}]";
    org.junit.Assert.assertEquals(expectedResult, result.toString());
}
```

## Manual configuration

You can configure LegacyTester instance by manual creating proxy on a class fields.
The qualifier `@Testee(qualifier = "constructMusic")` connects concrete instance of a LegacyTester and the concrete "pure" function.

```java
@Testee
public class MusicService implements Processing {
    private static LegacyTester tester = new LegacyTester(MusicService.class);

    @Autowired
    MessageBus messageBus;

    @Autowired
    SoundCloudDao soundCloudDao;

    @PostConstruct
    public void postConstruct() {
        this.messageBus = tester.createFieldProxy(MessageBus.class, new FieldInvocationHandler(messageBus, "sendAll").setFieldName("messageBus"));
        this.soundCloudDao = tester.createFieldProxy(SoundCloudDao.class, new FieldInvocationHandler(soundCloudDao).setFieldName("soundCloudDao"));
    }

    @Testee
    public List<Music> process(int integer, String str, boolean boot) {
        Map<String, TrackInfo> trackInfos = getTrackInfos();
        List<PlayHistory> listenedTracks = getListenedTracks();
        List<Music> collectedTracks = collectTracks(trackInfos, listenedTracks);
        sendMessages(collectedTracks);
        return collectedTracks;
    }

    @Testee(qualifier = "collectTracks")
    public List<Music> collectTracks(Map<String, TrackInfo> trackInfos,
                                     List<PlayHistory> listenedTracks) {
        List<Music> collectedTracks;
        //... no external dependencies
        Music music = new LegacyTester(MusicService.class)
                    .qualifier("constructMusic")
                    .test(() -> constructMusic(listenedTrack, trackInfo),
                            listenedTrack, trackInfo);
        collectedTracks.add(music);
        return collectedTracks;
    }

    @Testee(qualifier = "constructMusic")
    private Music constructMusic(PlayHistory track, TrackInfo info) {
        //no external dependencies
        return new Music(track, info);
    }
    //...
}
```

LegacyTester can generate a test even for a private functions.

```java
@Test
public void constructMusicTest() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    //Please create a test class manually if necessary
    ru.panfio.telescreen.handler.service.MusicService testClass = new ru.panfio.telescreen.handler.service.MusicService();

    //Given
    ru.panfio.telescreen.handler.model.soundcloud.PlayHistory track = ru.panfio.legacytester.util.JsonUtils.parse("{\"id\":1579855591640,\"externalId\":\"746114746\",\"listenTime\":\"2020-01-24T08:46:31.640Z\"}", new com.fasterxml.jackson.core.type.TypeReference<ru.panfio.telescreen.handler.model.soundcloud.PlayHistory>() {});
    ru.panfio.telescreen.handler.model.soundcloud.TrackInfo info = ru.panfio.legacytester.util.JsonUtils.parse("{\"id\":\"746114746\",\"artist\":\"vibe.digital\",\"title\":\"Episode 062 - A Look Forward at 2020\",\"url\":\"https://soundcloud.com/vibe-digital/episode062\"}", new com.fasterxml.jackson.core.type.TypeReference<ru.panfio.telescreen.handler.model.soundcloud.TrackInfo>() {});

    //When
    java.lang.reflect.Method method = testClass.getClass().getDeclaredMethod("constructMusic", ru.panfio.telescreen.handler.model.soundcloud.PlayHistory.class,ru.panfio.telescreen.handler.model.soundcloud.TrackInfo.class);
    method.setAccessible(true);
    ru.panfio.telescreen.handler.model.Music result = (ru.panfio.telescreen.handler.model.Music) method.invoke(testClass, track,info);

    //Then
    String expectedResult = "Music{id=1579855591640, externalId='746114746', type=SOUNDCLOUD, artist='vibe.digital', title='Episode 062 - A Look Forward at 2020', listenTime=2020-01-24T08:46:31.640Z, url='https://soundcloud.com/vibe-digital/episode062'}";
    org.junit.Assert.assertEquals(expectedResult, result.toString());
}
```