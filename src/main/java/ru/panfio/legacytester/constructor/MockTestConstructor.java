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

public class MockTestConstructor extends AbstractTestConstructor implements TestConstructor {
    private static final String MOCKITO_CLASS = "org.mockito.Mockito";
    private static final String ARGUMENT_CAPTOR_CLASS = "org.mockito.ArgumentCaptor";
    private static final String MOCK_FIELD_VARIABLE_SUFFIX = "Field";
    private static final String MOCK_PARAMETER_VARIABLE_SUFFIX = "PassedParameter";
    private static final String MOCK_RESULT_VARIABLE_SUFFIX = "ResultInvocation";
    private static final String CAPTOR_VARIABLE_SUFFIX = "Captor";
    private static final String CAPTOR_RESULT_VARIABLE_SUFFIX = "Result";
    private static final String CAPTOR_EXPECTED_RESULT_VARIABLE_SUFFIX = "ExpectedResult";
    private List<MethodCapture> capturedData;

    public MockTestConstructor(Class<?> testClass,
                               List<MethodCapture> capturedData,
                               MethodCapture testableMethodCapture) {
        super(testClass, testableMethodCapture);
        this.capturedData = capturedData;
    }

    @Override
    public String construct() {
        Method testMethod = testableMethodCapture.getMethod();
        Object result = testableMethodCapture.getResult();
        Object[] params = testableMethodCapture.getArguments();
        return new StringBuilder()
                .append(generateTestAnnotation())
                .append(generateTestMethodName(testMethod))
                .append(generateClassCreation(testClass))
                .append("\n")
                .append(createDependencyMocks(testClass))
                .append(Comment.GIVEN.text())
                .append(generateInputParams(testMethod, params))
                .append("\n")
                .append(generateMockDependencyInvocation())
                .append("\n")
                .append(Comment.WHEN.text())
                .append(generateTestMethodInvocation(testMethod))
                .append("\n")
                .append(Comment.THEN.text())
                .append(generateMockMethodInvocationChecks())
                .append("\n")
                .append(generateResultToString(result))
                .append(generateResultAssertToString())
                .append(generateCloseBracket())
                .toString();
    }

    private String createDependencyMocks(Class testClass) {
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
        final String typeName = field.getType().getTypeName();
        final String fieldName = field.getName();
        final String fieldVariable = fieldName + MOCK_FIELD_VARIABLE_SUFFIX;
        return mockCreation(typeName, fieldName, fieldVariable);
    }

    private String mockCreation(String typeName, String fieldName, String fieldVariable) {
        return bodySpace + typeName + " " + fieldName + " = " + MOCKITO_CLASS + ".mock(" + typeName + ".class);\n" +
                bodySpace + "Field " + fieldVariable + " = testClass.getClass().getDeclaredField(\"" + fieldName + "\");\n" +
                bodySpace + fieldVariable + ".setAccessible(true);\n" +
                bodySpace + fieldVariable + ".set(testClass, " + fieldName + ");\n";
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
        final String resultName = capture.methodName() + counter + MOCK_RESULT_VARIABLE_SUFFIX;
        final String type = capture.getMethod().getGenericReturnType().getTypeName();
        return generateObjectSerialization(result, resultName, type);
    }

    protected String generateCaptorPassedArgumentSerialization(MethodCapture methodCapture, int counter) {
        final Object[] arguments = methodCapture.getArguments();
        StringBuilder passedArguments = new StringBuilder();
        List<Parameter> methodParameters = getMethodParameters(methodCapture.getMethod());
        for (int index = 0; index < methodParameters.size(); index++) {
            Parameter parameter = methodParameters.get(index);
            final String type = parameter.getParameterizedType().getTypeName();
            final String parameterName = parameter.getName() + counter + MOCK_PARAMETER_VARIABLE_SUFFIX;

            passedArguments.append(generateObjectSerialization(arguments[index], parameterName, type));
        }
        return passedArguments.toString();
    }

    private String generateMockConfiguration(MethodCapture capture, int counter) {
        String fieldMock = capture.getFieldName();
        final String resultName = capture.methodName() + counter + MOCK_RESULT_VARIABLE_SUFFIX;
        final String methodName = capture.methodName();
        String parametersArguments = generateCaptorPassedArgumentNames(capture.getMethod(), counter);
        return mockInvocation(fieldMock, resultName, methodName, parametersArguments);
    }

    private String mockInvocation(String fieldMock, String resultName, String methodName, String parametersArguments) {
        return bodySpace + MOCKITO_CLASS + ".when(" + fieldMock + "." + methodName + "(" + parametersArguments + ")).thenReturn(" + resultName + ");\n";
    }

    protected String generateCaptorPassedArgumentNames(Method method, int counter) {
        String params = getMethodParameters(method)
                .stream()
                .map(parameter -> parameter.getName() + counter + MOCK_PARAMETER_VARIABLE_SUFFIX)
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
        final String type = methodInvocation.argumentTypeName(argumentName);
        String captor = methodInvocation.methodName() + argumentName + CAPTOR_VARIABLE_SUFFIX;
        return captorCreation(type, captor);
    }

    private String captorCreation(String type, String captor) {
        return bodySpace + "final " + ARGUMENT_CAPTOR_CLASS + "<" + type + "> " + captor + " = " + ARGUMENT_CAPTOR_CLASS + ".forClass(" + type + ".class);\n";
    }

    private String createMockVerifyConfiguration(MethodInvocation methodInvocation) {
        String fieldMock = methodInvocation.getFieldName();
        int invocationCount = methodInvocation.invocationCount();
        String methodName = methodInvocation.methodName();
        String captorArguments = generateCaptorArguments(methodInvocation.argumentNames(), methodName);
        return mockVerification(fieldMock, invocationCount, methodName, captorArguments);
    }

    private String mockVerification(String fieldMock, int invocationCount, String methodName, String captorArguments) {
        return bodySpace + MOCKITO_CLASS + ".verify(" + fieldMock + ", " + MOCKITO_CLASS + ".times(" + invocationCount + "))." + methodName + "(" + captorArguments + ");\n";
    }

    private String createArgumentCollection(MethodInvocation methodInvocation, String argumentName) {
        final String variable = methodInvocation.methodName() + argumentName;
        final String type = methodInvocation.argumentTypeName(argumentName);
        String captor = variable + CAPTOR_VARIABLE_SUFFIX;
        String result = variable + CAPTOR_RESULT_VARIABLE_SUFFIX;
        return captorArgumentCollection(type, result, captor);
    }

    private String captorArgumentCollection(String type, String result, String captor) {
        return bodySpace + "List<" + type + "> " + result + " = " + captor + ".getAllValues();\n";
    }

    private String createExpectedResults(MethodInvocation methodInvocation, String argumentName) {
        String result = methodInvocation.methodName() + argumentName + CAPTOR_EXPECTED_RESULT_VARIABLE_SUFFIX;
        final String passedParameters = methodInvocation.getArgument(argumentName).toString();
        return captorExpectedResult(result, passedParameters);
    }

    private String captorExpectedResult(String result, String passedParameters) {
        return bodySpace + "String " + result + " = \"" + passedParameters + "\";\n";
    }

    private String createMockAssertions(MethodInvocation methodInvocation, String argumentName) {
        final String variable = methodInvocation.methodName() + argumentName;
        String result = variable + CAPTOR_RESULT_VARIABLE_SUFFIX;
        String expectedResult = variable + CAPTOR_EXPECTED_RESULT_VARIABLE_SUFFIX;
        return mockAssertion(result, expectedResult);
    }

    private String mockAssertion(String result, String expectedResult) {
        return bodySpace + ASSERT_CLASS + ".assertEquals(" + expectedResult + ", " + result + ".toString());\n";
    }

    private String generateCaptorArguments(Collection<String> argumentNames, String methodName) {
        final String params = argumentNames.stream()
                .map(argumentName -> methodName + argumentName + CAPTOR_VARIABLE_SUFFIX + ".capture()")
                .reduce("", (acc, name) -> acc.concat(name + ","));
        return "".equals(params) ? "" : params.substring(0, params.length() - 1);
    }
}
