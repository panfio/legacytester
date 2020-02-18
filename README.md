
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
public void processTest777() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, java.lang.reflect.InvocationTargetException {
    //Please create a test class manually if necessary
    MusicService testClass = new MusicService(null,null);

    MessageBus messageBus = Mockito.mock(MessageBus.class);
    Field messageBusField = testClass.getClass().getDeclaredField("messageBus");
    messageBusField.setAccessible(true);
    messageBusField.set(testClass, messageBus);
    SoundCloudDao soundCloudDao = Mockito.mock(SoundCloudDao.class);
    Field soundCloudDaoField = testClass.getClass().getDeclaredField("soundCloudDao");
    soundCloudDaoField.setAccessible(true);
    soundCloudDaoField.set(testClass, soundCloudDao);
    //Given

    Map<String, TrackInfo> tracksInfo0ResultInvocation = JsonUtils.parse("{\"732251920\":{\"id\":\"732251920\",\"artist\":\"Vesky\",\"title\":\"Leaving\",\"url\":\"https://soundcloud.com/vskymusic/leaving\"},\"746114746\":{\"id\":\"746114746\",\"artist\":\"vibe.digital\",\"title\":\"Episode 062 - A Look Forward at 2020\",\"url\":\"https://soundcloud.com/vibe-digital/episode062\"},\"745949599\":{\"id\":\"745949599\",\"artist\":\"-Bucky-\",\"title\":\"Bucky - Night Racer\",\"url\":\"https://soundcloud.com/bucky-music/bucky-night-racer\"}}", new TypeReference<Map<String, TrackInfo>>() {});
    org.mockito.Mockito.when(soundCloudDao.tracksInfo()).thenReturn(tracksInfo0ResultInvocation);
    List<PlayHistory> recentlyPlayed1ResultInvocation = JsonUtils.parse("[{\"id\":1579856369307,\"externalId\":\"732251920\",\"listenTime\":\"2020-01-24T08:59:29.307Z\"},{\"id\":1579856079704,\"externalId\":\"745949599\",\"listenTime\":\"2020-01-24T08:54:39.704Z\"},{\"id\":1579855591640,\"externalId\":\"746114746\",\"listenTime\":\"2020-01-24T08:46:31.640Z\"}]", new TypeReference<List<PlayHistory>>() {});
    org.mockito.Mockito.when(soundCloudDao.recentlyPlayed()).thenReturn(recentlyPlayed1ResultInvocation);

    //When
    testClass.process();

    //Then
    final ArgumentCaptor<String> sendAllarg0Captor = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<ArrayList> sendAllarg1Captor = ArgumentCaptor.forClass(ArrayList.class);
    Mockito.verify(messageBus, Mockito.times(1)).sendAll(sendAllarg0Captor.capture(),sendAllarg1Captor.capture());
    List<String> sendAllarg0Result = sendAllarg0Captor.getAllValues();
    List<ArrayList> sendAllarg1Result = sendAllarg1Captor.getAllValues();
    String sendAllarg0ExpectedResult = "[music]";
    String sendAllarg1ExpectedResult = "[[Music{id=1579856369307, externalId='732251920', type=SOUNDCLOUD, artist='Vesky', title='Leaving', listenTime=2020-01-24T08:59:29.307Z, url='https://soundcloud.com/vskymusic/leaving'}, Music{id=1579856079704, externalId='745949599', type=SOUNDCLOUD, artist='-Bucky-', title='Bucky - Night Racer', listenTime=2020-01-24T08:54:39.704Z, url='https://soundcloud.com/bucky-music/bucky-night-racer'}, Music{id=1579855591640, externalId='746114746', type=SOUNDCLOUD, artist='vibe.digital', title='Episode 062 - A Look Forward at 2020', listenTime=2020-01-24T08:46:31.640Z, url='https://soundcloud.com/vibe-digital/episode062'}]]";
    Assertions.assertEquals(sendAllarg0ExpectedResult, sendAllarg0Result.toString());
    Assertions.assertEquals(sendAllarg1ExpectedResult, sendAllarg1Result.toString());

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
public void constructMusicTest66121() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, java.lang.reflect.InvocationTargetException {
    //Please create a test class manually if necessary
    MusicService testClass = new MusicService(null,null);

    //Given
    PlayHistory arg0 = JsonUtils.parse("{\"id\":1579856369307,\"externalId\":\"732251920\",\"listenTime\":\"2020-01-24T08:59:29.307Z\"}", new TypeReference<PlayHistory>() {});
    TrackInfo arg1 = JsonUtils.parse("{\"id\":\"732251920\",\"artist\":\"Vesky\",\"title\":\"Leaving\",\"url\":\"https://soundcloud.com/vskymusic/leaving\"}", new TypeReference<TrackInfo>() {});

    //When
    Method constructMusic = testClass.getClass().getDeclaredMethod("constructMusic", PlayHistory.class,TrackInfo.class);
    constructMusic.setAccessible(true);
    Class result = (Class) constructMusic.invoke(testClass, arg0,arg1);

    //Then
    String expectedResult = "Music{id=1579856369307, externalId='732251920', type=SOUNDCLOUD, artist='Vesky', title='Leaving', listenTime=2020-01-24T08:59:29.307Z, url='https://soundcloud.com/vskymusic/leaving'}";
    Assertions.assertEquals(expectedResult, result.toString());
}
```