package ru.panfio.legacytester.constructor;

import ru.panfio.legacytester.MethodCapture;
import ru.panfio.legacytester.MethodInvocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
                .append(generateMockDependencyInvocationChecks())
                .append("\n")
                .append(generateResultToString(result))
                .append(generateResultAssertToString())
                .append(generateCloseBracket())
                .toString();
    }

    private String mockCreation(String typeName, String fieldName, String fieldVariable) {
        return bodySpace + typeName + " " + fieldName + " = " + MOCKITO_CLASS + ".mock(" + typeName + ".class);\n" +
                bodySpace + "Field " + fieldVariable + " = testClass.getClass().getDeclaredField(\"" + fieldName + "\");\n" +
                bodySpace + fieldVariable + ".setAccessible(true);\n" +
                bodySpace + fieldVariable + ".set(testClass, " + fieldName + ");\n";
    }

    private String mockInvocation(String fieldMock, String resultName, String methodName, String parametersArguments) {
        return bodySpace + MOCKITO_CLASS + ".when(" + fieldMock + "." + methodName + "(" + parametersArguments + ")).thenReturn(" + resultName + ");\n";
    }

    private String createDependencyMocks(Class testClass) {
        StringBuilder mocks = new StringBuilder();
        for (Field field : getClassFields(testClass)) {
            final String typeName = field.getType().getTypeName();
            if (Modifier.isStatic(field.getModifiers()) || "ru.panfio.legacytester.LegacyTester".equals(typeName)) {
                continue;
            }

            final String fieldName = field.getName();
            String fieldVariable = fieldName + MOCK_FIELD_VARIABLE_SUFFIX;
            mocks.append(mockCreation(typeName, fieldName, fieldVariable));
        }
        return mocks.toString();
    }

    private String generateMockDependencyInvocation() {
        List<MethodCapture> dependenciesInvocations = MethodCapture.dependenciesInvocations(capturedData);
        StringBuilder mocksInvocations = new StringBuilder();
        int counter = 0;
        for (MethodCapture capture : dependenciesInvocations) {
            counter++;
            Method method = capture.getMethod();
            final String methodName = method.getName();
            final String type = method.getGenericReturnType().getTypeName();
            Object result = capture.getResult();
            final String resultName = methodName + counter + MOCK_RESULT_VARIABLE_SUFFIX;
            mocksInvocations
                    .append(generateObjectSerialization(result, resultName, type))
                    .append(generateCaptorPassedArgumentSerialization(capture, counter))
                    .append(generateMockConfiguration(capture, counter, resultName));
        }
        return mocksInvocations.toString();
    }

    private String generateMockConfiguration(MethodCapture capture, int counter, String resultName) {
        Method method = capture.getMethod();
        String fieldMock = capture.getFieldName();
        final String methodName = method.getName();
        String parametersArguments = generateCaptorPassedArgumentNames(method, counter);
        return mockInvocation(fieldMock, resultName, methodName, parametersArguments);
    }

    protected String generateCaptorPassedArgumentNames(Method method, int counter) {
        StringBuilder passedArguments = new StringBuilder();
        for (Parameter parameter : getMethodParameters(method)) {
            final String parameterName = parameter.getName() + counter + MOCK_PARAMETER_VARIABLE_SUFFIX;
            passedArguments.append(parameterName).append(",");
        }
        String params = passedArguments.toString();
        return "".equals(params) ? "" : params.substring(0, params.length() - 1);
    }

    protected String generateCaptorPassedArgumentSerialization(MethodCapture methodCapture, int counter) {
        List<Parameter> methodParameters = getMethodParameters(methodCapture.getMethod());
        final Object[] arguments = methodCapture.getArguments();
        StringBuilder passedArguments = new StringBuilder();
        for (int index = 0; index < methodParameters.size(); index++) {
            Parameter parameter = methodParameters.get(index);
            final String type = parameter.getParameterizedType().getTypeName();
            final String parameterName = parameter.getName() + counter + MOCK_PARAMETER_VARIABLE_SUFFIX;

            passedArguments.append(generateObjectSerialization(arguments[index], parameterName, type));
        }
        return passedArguments.toString();
    }

    private String generateMockDependencyInvocationChecks() {
        List<MethodCapture> affected = MethodCapture.affectedInvocations(capturedData);
        List<MethodInvocation> methodInvocations = MethodInvocation.of(affected);
        return methodInvocations.stream()
                .map(this::testCreation)
                .collect(Collectors.joining());
    }

    private String testCreation(MethodInvocation methodInvocation) {
        return methodInvocation.forEachArgument(this::createCaptors)
                + methodInvocation.apply(this::createMockVerifyConfiguration)
                + methodInvocation.forEachArgument(this::createArgumentCollection)
                + methodInvocation.forEachArgument(this::createExpectedResults)
                + methodInvocation.forEachArgument(this::createMockAssertions);
    }

    private String createCaptors(MethodInvocation methodInvocation, String argumentName) {
        String methodName = methodInvocation.getMethod().getName();
        final String type = methodInvocation.getArguments().get(argumentName).get(0).getClass().getTypeName();
        String captor = methodName + argumentName + CAPTOR_VARIABLE_SUFFIX;
        return captorCreation(type, captor);
    }

    private String captorCreation(String type, String captor) {
        return bodySpace + "final " + ARGUMENT_CAPTOR_CLASS + "<" + type + "> " + captor + " = " + ARGUMENT_CAPTOR_CLASS + ".forClass(" + type + ".class);\n";
    }

    private String createMockVerifyConfiguration(MethodInvocation methodInvocation) {
        String fieldMock = methodInvocation.getFieldName();
        int invocationCount = methodInvocation.invocationCount();
        String methodName = methodInvocation.getMethod().getName();
        String captorArguments = generateCaptorArguments(methodInvocation.argumentNames(), methodName);
        return mockVerification(fieldMock, invocationCount, methodName, captorArguments);
    }

    private String mockVerification(String fieldMock, int invocationCount, String methodName, String captorArguments) {
        return bodySpace + MOCKITO_CLASS + ".verify(" + fieldMock + ", " + MOCKITO_CLASS + ".times(" + invocationCount + "))." + methodName + "(" + captorArguments + ");\n";
    }

    private String createArgumentCollection(MethodInvocation methodInvocation, String argumentName) {
        Map<String, List<Object>> arguments = methodInvocation.getArguments();
        final String variable = methodInvocation.getMethod().getName() + argumentName;
        final String type = arguments.get(argumentName).get(0).getClass().getTypeName();
        String captor = variable + CAPTOR_VARIABLE_SUFFIX;
        String result = variable + CAPTOR_RESULT_VARIABLE_SUFFIX;
        return captorArgumentCollection(type, result, captor);
    }

    private String captorArgumentCollection(String type, String result, String captor) {
        return bodySpace + "List<" + type + "> " + result + " = " + captor + ".getAllValues();\n";
    }

    private String createExpectedResults(MethodInvocation methodInvocation, String argumentName) {
        String methodName = methodInvocation.getMethod().getName();
        String result = methodName + argumentName + CAPTOR_EXPECTED_RESULT_VARIABLE_SUFFIX;
        final String passedParameters = methodInvocation.getArguments().get(argumentName).toString();
        return captorExpectedResult(result, passedParameters);
    }

    private String captorExpectedResult(String result, String passedParameters) {
        return bodySpace + "String " + result + " = \"" + passedParameters + "\";\n";
    }

    private String createMockAssertions(MethodInvocation methodInvocation, String argumentName) {
        final String variable = methodInvocation.getMethod().getName() + argumentName;
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
