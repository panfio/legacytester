package ru.panfio.legacytester;

import lombok.SneakyThrows;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import ru.panfio.legacytester.constructor.ConstructorConfiguration;
import ru.panfio.legacytester.constructor.MockTestConstructor;
import ru.panfio.legacytester.constructor.TestConstructor;
import ru.panfio.legacytester.constructor.ConstructorSupplier;
import ru.panfio.legacytester.spring.MethodInvocationInterceptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static ru.panfio.legacytester.util.ReflectionUtils.containAnnotation;

public class LegacyTester {
    private final Class<?> testClass;
    private String qualifier = "default";
    private Map<String, FieldInvocationHandler> handlers = new HashMap<>();
    private ConstructorSupplier<Class, ConstructorConfiguration, List<MethodCapture>, TestConstructor> testConstructorSupplier;
    private Consumer<TestConstructor> testHandler;
    private Supplier<ConstructorConfiguration> constructorConfigSupplier;

    public LegacyTester(Class<?> testClass) {
        this.testClass = testClass;
    }

    public LegacyTester testHandler(Consumer<TestConstructor> testHandler) {
        this.testHandler = testHandler;
        return this;
    }

    public LegacyTester constructorConfig(Supplier<ConstructorConfiguration> constructorConfig) {
        this.constructorConfigSupplier = constructorConfig;
        return this;
    }

    public LegacyTester testConstructor(ConstructorSupplier<Class, ConstructorConfiguration, List<MethodCapture>, TestConstructor> supplier) {
        this.testConstructorSupplier = supplier;
        return this;
    }

    public String getQualifier() {
        return qualifier;
    }

    public LegacyTester qualifier(String qualifier) {
        this.qualifier = qualifier;
        return this;
    }

    @Deprecated
    public void test(Object[] params) {
        generateTest(null, null, params);
    }

    @Deprecated
    public void test(Object result, Object[] params) {
        generateTest(result, null, params);
    }

    @SneakyThrows
    public void test(ThrowableRunnable testMethod, Object... params) {
        try {
            testMethod.run();
            generateTest(null, null, params);
        } catch (Throwable exception) {
            generateTest(null, exception, params);
            throw exception;
        }
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
    @SneakyThrows
    public <R> R test(ThrowableSupplier<R> testMethod, Object... params) {
        try {
            R result = testMethod.get();
            generateTest(result, null, params);
            return result;
        } catch (Throwable exception) {
            generateTest(null, exception, params);
            throw exception;
        }
    }

    private void generateTest(Object result, Throwable exception, Object... params) {
        Method testMethod = getTestableMethod();
        if (testMethod == null) {
            System.out.println("Please annotate testable method with @Testee");
            return;
        }
        List<MethodCapture> capturedData = MethodCapture.collectFrom(handlers);
        capturedData.add(MethodCapture.builder()
                .method(testMethod)
                .type(MethodCapture.Type.TEST)
                .arguments(params)
                .result(result)
                .exception(exception)
                .build());

        ConstructorConfiguration constructorConfig = getConstructorConfiguration();
        TestConstructor testConstructor = getTestConstructor(capturedData, constructorConfig);
        if (testHandler != null) {
            testHandler.accept(testConstructor);
            return;
        }
        // default
        String testText = testConstructor.construct();
        printGeneratedTest(testText);
    }

    private TestConstructor getTestConstructor(List<MethodCapture> capturedData,
                                               ConstructorConfiguration constructorConfig) {
        if (testConstructorSupplier != null) {
            return testConstructorSupplier.get(testClass, constructorConfig, capturedData);
        } else {
            return new MockTestConstructor(testClass,constructorConfig, capturedData);
        }
    }

    private ConstructorConfiguration getConstructorConfiguration() {
        if (constructorConfigSupplier != null) {
            return constructorConfigSupplier.get();
        } else {
            return ConstructorConfiguration.builder().build();
        }
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
     * @param target          proxy target .class
     * @param handler         invocation handler LegacyTesterProxy
     * @param otherInterfaces another interfaces for implementation
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T fieldProxy(Class<? extends T> target, InvocationHandler handler, Class<?>... otherInterfaces) {
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
    public static Object classProxy(Object bean, LegacyTester tester) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(bean.getClass());
        Callback saveCallback = new MethodInvocationInterceptor(bean, tester);
        Callback[] callbacks = new Callback[]{saveCallback};
        enhancer.setCallbacks(callbacks);
        return enhancer.create();
    }
}
