package ru.panfio.legacytester.constructor;

import ru.panfio.legacytester.MethodCapture;
import ru.panfio.legacytester.MethodInvocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static ru.panfio.legacytester.util.ReflectionUtils.*;

public class MockTestConstructor implements TestConstructor {
    private final List<MethodCapture> capturedData;
    private final Class<?> testClass;
    private final Constructor constructor;
    private ConstructorConfiguration conf;

    public MockTestConstructor(Class<?> testClass,
                               List<MethodCapture> capturedData) {
        this.testClass = testClass;
        final Constructor constructor = new Constructor(testClass, MethodCapture.testInvocation(capturedData));
        this.constructor = constructor;
        this.conf = constructor.configuration();
        this.capturedData = capturedData;
    }

    public TestConstructor configuration(ConstructorConfiguration conf) {
        // TODO: refactor it
        this.constructor.configuration(conf);
        this.conf = conf;
        return this;
    }

    @Override
    public String construct() {
        return new StringBuilder()
                .append(constructor.generateTestAnnotation())
                .append(constructor.generateTestMethodName())
                .append(constructor.generateClassCreation())
                .append("\n")
                .append(createDependencyMocks())
                .append(Comment.GIVEN.text(conf.bodySpace()))
                .append(constructor.generateInputParams())
                .append("\n")
                .append(generateMockDependencyInvocation())
                .append("\n")
                .append(Comment.WHEN.text(conf.bodySpace()))
                .append(constructor.generateTestMethodInvocation())
                .append("\n")
                .append(Comment.THEN.text(conf.bodySpace()))
                .append(generateMockMethodInvocationChecks())
                .append("\n")
                .append(constructor.generateResultToString())
                .append(constructor.generateAssertions())
                .append(constructor.generateCloseBracket())
                .toString();
    }

    private String createDependencyMocks() {
        return getClassFields(testClass).stream()
                .filter(this::isaFieldNeededAProxy)
                .map(this::createMockString)
                .collect(Collectors.joining());
    }

    private boolean isaFieldNeededAProxy(Field field) {
        final String typeName = field.getType().getTypeName();
        if (Modifier.isStatic(field.getModifiers()) || "ru.panfio.legacytester.LegacyTester".equals(typeName)) {
            return false;
        }
        return true;
    }

    private String createMockString(Field field) {
        if (capturedData.size() == 1) {
            // no field proxy data and mock is redundant
            return "";
        }
        final String typeName = conf.type(field.getType());
        final String fieldName = field.getName();
        final String fieldVariable = fieldName + conf.MOCK_FIELD_VARIABLE_SUFFIX;
        return mockCreation(typeName, fieldName, fieldVariable);
    }

    private String mockCreation(String typeName, String fieldName, String fieldVariable) {
        return conf.bodySpace() + typeName + " " + fieldName + " = " + conf.mockitoClass() + ".mock(" + typeName + ".class);\n" +
                conf.bodySpace() + conf.type(Field.class)+ " " + fieldVariable + " = testClass.getClass().getDeclaredField(\"" + fieldName + "\");\n" +
                conf.bodySpace() + fieldVariable + ".setAccessible(true);\n" +
                conf.bodySpace() + fieldVariable + ".set(testClass, " + fieldName + ");\n";
    }

    private String generateMockDependencyInvocation() {
        List<MethodCapture> dependenciesInvocations = MethodCapture.dependenciesInvocations(capturedData);
        StringBuilder mocksInvocations = new StringBuilder();
        for (int counter = 0; counter < dependenciesInvocations.size(); counter++) {
            MethodCapture capture = dependenciesInvocations.get(counter);
            mocksInvocations
                    .append(generateMockInvocationReturnValue(capture, counter))
                    .append(generateCaptorPassedArgumentSerialization(capture, counter))
                    .append(generateMockConfiguration(capture, counter));
        }
        return mocksInvocations.toString();
    }

    private String generateMockInvocationReturnValue(MethodCapture capture, int counter) {
        Object result = capture.getResult();
        final String resultName = capture.methodName() + counter + conf.MOCK_RESULT_VARIABLE_SUFFIX;
        final String type = conf.type(capture.getMethod());
        return constructor.generateObjectSerialization(result, resultName, type);
    }

    protected String generateCaptorPassedArgumentSerialization(MethodCapture methodCapture, int counter) {
        final Object[] arguments = methodCapture.getArguments();
        StringBuilder passedArguments = new StringBuilder();
        List<Parameter> methodParameters = getMethodParameters(methodCapture.getMethod());
        for (int index = 0; index < methodParameters.size(); index++) {
            Parameter parameter = methodParameters.get(index);
            final String parameterName = parameter.getName() + counter + conf.MOCK_PARAMETER_VARIABLE_SUFFIX;
            passedArguments.append(constructor.generateObjectSerialization(arguments[index], parameterName, conf.type(parameter)));
        }
        return passedArguments.toString();
    }

    private String generateMockConfiguration(MethodCapture capture, int counter) {
        String fieldMock = capture.getFieldName();
        final String resultName = capture.methodName() + counter + conf.MOCK_RESULT_VARIABLE_SUFFIX;
        final String methodName = capture.methodName();
        String parametersArguments = generateCaptorPassedArgumentNames(capture.getMethod(), counter);
        return mockInvocation(fieldMock, resultName, methodName, parametersArguments);
    }

    private String mockInvocation(String fieldMock, String resultName, String methodName, String parametersArguments) {
        return conf.bodySpace() + conf.MOCKITO_CLASS + ".when(" + fieldMock + "." + methodName + "(" + parametersArguments + ")).thenReturn(" + resultName + ");\n";
    }

    protected String generateCaptorPassedArgumentNames(Method method, int counter) {
        String params = getMethodParameters(method)
                .stream()
                .map(parameter -> parameter.getName() + counter + conf.MOCK_PARAMETER_VARIABLE_SUFFIX)
                .map(parameterName -> parameterName + ",")
                .collect(Collectors.joining());
        return "".equals(params) ? "" : params.substring(0, params.length() - 1);
    }

    private String generateMockMethodInvocationChecks() {
        List<MethodCapture> affected = MethodCapture.affectedInvocations(capturedData);
        List<MethodInvocation> methodInvocations = MethodInvocation.of(affected);
        return methodInvocations.stream()
                .map(this::generateMockMethodInvocationChecksString)
                .collect(Collectors.joining());
    }

    private String generateMockMethodInvocationChecksString(MethodInvocation methodInvocation) {
        return methodInvocation.forEachArgument(this::createCaptors)
                + methodInvocation.apply(this::createMockVerifyConfiguration)
                + methodInvocation.forEachArgument(this::createArgumentCollection)
                + methodInvocation.forEachArgument(this::createExpectedResults)
                + methodInvocation.forEachArgument(this::createMockAssertions);
    }

    private String createCaptors(MethodInvocation methodInvocation, String argumentName) {
        final String type = conf.type(methodInvocation.argumentType(argumentName));
        String captor = methodInvocation.methodName() + argumentName + conf.CAPTOR_VARIABLE_SUFFIX;
        return captorCreation(type, captor);
    }

    private String captorCreation(String type, String captor) {
        return conf.bodySpace() + "final " + conf.captorClass() + "<" + type + "> " + captor + " = " + conf.captorClass() + ".forClass(" + type + ".class);\n";
    }

    private String createMockVerifyConfiguration(MethodInvocation methodInvocation) {
        String fieldMock = methodInvocation.getFieldName();
        int invocationCount = methodInvocation.invocationCount();
        String methodName = methodInvocation.methodName();
        String captorArguments = generateCaptorArguments(methodInvocation.argumentNames(), methodName);
        return mockVerification(fieldMock, invocationCount, methodName, captorArguments);
    }

    private String mockVerification(String fieldMock, int invocationCount, String methodName, String captorArguments) {
        return conf.bodySpace() + conf.mockitoClass() + ".verify(" + fieldMock + ", " + conf.mockitoClass() + ".times(" + invocationCount + "))." + methodName + "(" + captorArguments + ");\n";
    }

    private String createArgumentCollection(MethodInvocation methodInvocation, String argumentName) {
        final String variable = methodInvocation.methodName() + argumentName;
        final String type = conf.type(methodInvocation.argumentType(argumentName));
        String captor = variable + conf.CAPTOR_VARIABLE_SUFFIX;
        String result = variable + conf.CAPTOR_RESULT_VARIABLE_SUFFIX;
        return captorArgumentCollection(type, result, captor);
    }

    private String captorArgumentCollection(String type, String result, String captor) {
        return conf.bodySpace() + "List<" + type + "> " + result + " = " + captor + ".getAllValues();\n";
    }

    private String createExpectedResults(MethodInvocation methodInvocation, String argumentName) {
        String result = methodInvocation.methodName() + argumentName + conf.CAPTOR_EXPECTED_RESULT_VARIABLE_SUFFIX;
        final String passedParameters = methodInvocation.getArgument(argumentName).toString();
        return captorExpectedResult(result, passedParameters);
    }

    private String captorExpectedResult(String result, String passedParameters) {
        return conf.bodySpace() + "String " + result + " = \"" + passedParameters + "\";\n";
    }

    private String createMockAssertions(MethodInvocation methodInvocation, String argumentName) {
        final String variable = methodInvocation.methodName() + argumentName;
        String result = variable + conf.CAPTOR_RESULT_VARIABLE_SUFFIX;
        String expectedResult = variable + conf.CAPTOR_EXPECTED_RESULT_VARIABLE_SUFFIX;
        return mockAssertion(result, expectedResult);
    }

    private String mockAssertion(String result, String expectedResult) {
        return conf.bodySpace() + conf.assertionClass() + ".assertEquals(" + expectedResult + ", " + result + ".toString());\n";
    }

    private String generateCaptorArguments(Collection<String> argumentNames, String methodName) {
        final String params = argumentNames.stream()
                .map(argumentName -> methodName + argumentName + conf.CAPTOR_VARIABLE_SUFFIX + ".capture()")
                .reduce("", (acc, name) -> acc.concat(name + ","));
        return "".equals(params) ? "" : params.substring(0, params.length() - 1);
    }
}
