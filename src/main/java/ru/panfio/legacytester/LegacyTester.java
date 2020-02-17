package ru.panfio.legacytester;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import ru.panfio.legacytester.constructor.ConstructorConfiguration;
import ru.panfio.legacytester.constructor.MockTestConstructor;
import ru.panfio.legacytester.constructor.TestConstructor;
import ru.panfio.legacytester.spring.MethodInvocationInterceptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.panfio.legacytester.util.ReflectionUtils.containAnnotation;

public class LegacyTester {
    private final Class<?> testClass;
    private String qualifier = "default";
    private Map<String, FieldInvocationHandler> handlers = new HashMap<>();
    private ConstructorConfiguration conf = new ConstructorConfiguration();

    public LegacyTester(Class<?> testClass) {
        this.testClass = testClass;
    }

    public LegacyTester constructorConfiguration(ConstructorConfiguration conf) {
        this.conf = conf;
        System.out.println("sssset conf " + conf);
        return this;
    }

    public String getQualifier() {
        return qualifier;
    }

    public LegacyTester qualifier(String qualifier) {
        this.qualifier = qualifier;
        return this;
    }

    /**
     * Tests a function with no return value.
     * <pre>{@code
     *    @Testee
     *    public void process(Param param0);
     * }</pre>
     *
     * @param params input parameters
     */
    public void test(Object[] params) {
        generateTest(null, params);
    }

    public void test(Object result, Object[] params) {
        generateTest(result, params);
    }

    /**
     * Captures input and output values and generates a test method.
     * Lets assume you want to test the function:
     * <pre>{@code
     *    @Testee
     *    private List<Music> collectTracks(Map<String, TrackInfo> trackInfos,
     *                                   List<PlayHistory> listenedTracks);
     * }</pre>
     * Then wrap a real function call in lambda and pass it as a parameter
     * along with variables of a real call (order is important).
     * <pre>{@code
     *    List<Music> collectedTracks = tester.test(
     *                 () -> collectTracks(trackInfos, listenedTracks),
     *                 trackInfos,
     *                 listenedTracks);
     * }</pre>
     * Then run the application and invoke testing function with correct/incorrect/tested data.
     * Check the app logs and copy generated test methods into test classes.
     *
     * @param testMethod wrap
     * @param params     input parameters
     * @param <R>        result type of a real function
     * @return result of a real function call
     */
    public <R> R test(Supplier<R> testMethod, Object... params) {
        R result = testMethod.get();
        generateTest(result, params);
        return result;
    }

    private void generateTest(Object result, Object... params) {
        Method testMethod = getTestableMethod();
        if (testMethod == null) {
            System.out.println("Please annotate testable method with @Testee");
            return;
        }
        List<MethodCapture> capturedData = collectCapturedData();
        capturedData.add(new MethodCapture(testMethod, MethodCapture.Type.TEST, params, result));

        System.out.println("leg tester " + qualifier);
        System.out.println("leg tester " + conf);
        final TestConstructor testConstructor = new MockTestConstructor(testClass, capturedData).configuration(conf);
        String testText = testConstructor.construct();
        printGeneratedTest(testText);
    }

    private Method getTestableMethod() {
        for (Method method : testClass.getDeclaredMethods()) {
            if (!containAnnotation(method, Testee.class)) {
                continue;
            }
            Testee testeeAnnotation = method.getAnnotation(Testee.class);
            if (testeeAnnotation.qualifier().equals(qualifier)) {
                return method;
            }
        }
        return null;
    }

    public void clearInvocations() {
        handlers.values().forEach(FieldInvocationHandler::clearCapturedInvocations);
    }

    private List<MethodCapture> collectCapturedData() {
        List<MethodCapture> capturedData = new ArrayList<>();
        for (Map.Entry<String, FieldInvocationHandler> entry : handlers.entrySet()) {
            List<MethodCapture> data = entry.getValue()
                    .getCapturedInvocations()
                    .stream()
                    .map(methodCapture -> methodCapture.setFieldName(entry.getKey()))
                    .collect(Collectors.toList());
            capturedData.addAll(data);
        }
        return capturedData;
    }

    private void printGeneratedTest(String test) {
        String header =
                        "//================================================================//\n" +
                        "//======================== GENERATED TEST ========================//\n" +
                        "//================================================================//\n";
        String footer =
                        "//////////////////////////////// END ///////////////////////////////\n";
        System.out.println(header + test + "\n" + footer);
    }

    /**
     * Creates a proxy instance for a dependency to capture arguments.
     * Example with manual constructor creation:
     * <pre>{@code
     *     LegacyTester tester = new LegacyTester(MyTestClass.class);
     *     private final MessageBus messageBus;
     *     public MyTestClass(MessageBus messageBus) {
     *         this.messageBus = tester.createFieldProxy(MessageBus.class, new FieldInvocationHandler(messageBus, "send").setFieldName(messageBus));
     *     }
     * }</pre>
     * <p>
     * Example with @Autowired dependency:
     * <pre>{@code
     *     LegacyTester tester = new LegacyTester(MyTestClass.class);
     *     @Autowired
     *     private MessageBus messageBus;
     *     @PostConstruct
     *     public void postConstruct() {
     *         this.messageBus = tester.createFieldProxy(MessageBus.class, new FieldInvocationHandler(messageBus, "send").setFieldName(messageBus));
     *     }
     * }</pre>
     *
     * @param target      proxy target .class
     * @param handler     invocation handler LegacyTesterProxy
     * @param otherInterfaces another interfaces for implementation
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T createFieldProxy(Class<? extends T> target, InvocationHandler handler, Class<?>... otherInterfaces) {
        FieldInvocationHandler invocationHandler = (FieldInvocationHandler) handler;
        handlers.put(invocationHandler.getFieldName(), invocationHandler);
        Class<?>[] allInterfaces =
                Stream.concat(Stream.of(target), Stream.of(otherInterfaces))
                        .distinct()
                        .toArray(Class<?>[]::new);
        return (T) Proxy.newProxyInstance(target.getClassLoader(), allInterfaces, handler);
    }

    /**
     * Creates a CGLib proxy instance for the test class.
     *
     * @param bean   target bean
     * @param tester LegacyTester object
     * @return proxy instance
     */
    public static Object createClassProxy(Object bean, LegacyTester tester) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(bean.getClass());
        Callback saveCallback = new MethodInvocationInterceptor(bean, tester);
        Callback[] callbacks = new Callback[]{saveCallback};
        enhancer.setCallbacks(callbacks);
        return enhancer.create();
    }
}
