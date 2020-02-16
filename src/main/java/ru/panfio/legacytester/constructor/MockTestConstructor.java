package ru.panfio.legacytester.constructor;

import ru.panfio.legacytester.MethodCapture;
import ru.panfio.legacytester.MethodInvocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private String generateMockDependencyInvocation() {
        List<MethodCapture> dependenciesInvocations = getDependenciesInvocations();
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
        return bodySpace + MOCKITO_CLASS + ".when(" + fieldMock + "." + methodName + "(" + parametersArguments +")).thenReturn(" + resultName + ");\n";
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

    protected String generateCaptorPassedArgumentNames(Method method, int counter) {
        List<Parameter> methodParameters = getMethodParameters(method);
        StringBuilder passedArguments = new StringBuilder();
        for (Parameter parameter : methodParameters) {
            final String parameterName = parameter.getName() + counter + MOCK_PARAMETER_VARIABLE_SUFFIX;
            passedArguments.append(parameterName).append(",");
        }
        String params = passedArguments.toString();
        return "".equals(params) ? "" : params.substring(0, params.length() - 1);
    }

    private String generateMockDependencyInvocationChecks() {
        List<MethodCapture> affected = getAffectedDependenciesInvocations();
        List<MethodInvocation> methodInvocations = collectMethodInvocations(affected);
        StringBuilder affectedInvocations = new StringBuilder();
        for (MethodInvocation methodInvocation : methodInvocations) {
            affectedInvocations.append(createCaptors(methodInvocation))
                    .append(createMockVerifyConfiguration(methodInvocation))
                    .append(createArgumentCollection(methodInvocation))
                    .append(createExpectedResults(methodInvocation))
                    .append("\n")
                    .append(createMockAssertions(methodInvocation));
        }
        return affectedInvocations.toString();
    }

    private List<MethodInvocation> collectMethodInvocations(List<MethodCapture> affected) {
        List<MethodInvocation> methodInvocations = new ArrayList<>();
        for (MethodCapture methodCapture : affected) {
            if (isaMethodInvocationsContainMethod(methodInvocations, methodCapture)) {
                addToExistingInvocation(methodInvocations, methodCapture);
            } else {
                addNewInvocation(methodInvocations, methodCapture);
            }
        }
        return methodInvocations;
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
            String mock = bodySpace + typeName + " " + fieldName + " = " + MOCKITO_CLASS + ".mock(" + typeName + ".class);\n" +
                    bodySpace + "Field " + fieldVariable + " = testClass.getClass().getDeclaredField(\"" + fieldName + "\");\n" +
                    bodySpace + fieldVariable + ".setAccessible(true);\n" +
                    bodySpace + fieldVariable + ".set(testClass, " + fieldName + ");\n";
            mocks.append(mock).append("\n");
        }
        return mocks.toString();
    }

    private String createCaptors(MethodInvocation methodInvocation) {
        Map<String, List<Object>> arguments = methodInvocation.getArguments();
        Set<String> argumentNames = arguments.keySet();
        StringBuilder captors = new StringBuilder();
        for (String argumentName : argumentNames) {
            Method method = methodInvocation.getMethod();
            String methodName = method.getName();
            final String type = arguments.get(argumentName).get(0).getClass().getTypeName();
            String captor = methodName + argumentName + CAPTOR_VARIABLE_SUFFIX;
            String argumentCaptor = bodySpace + "final " + ARGUMENT_CAPTOR_CLASS + "<" + type + "> " + captor + " = " + ARGUMENT_CAPTOR_CLASS + ".forClass(" + type + ".class);\n";
            captors.append(argumentCaptor);
        }
        return captors.toString();
    }

    private String createMockVerifyConfiguration(MethodInvocation methodInvocation) {
        Map<String, List<Object>> arguments = methodInvocation.getArguments();
        Set<String> argumentNames = arguments.keySet();
        int numberOfInvocation = getNumberOfInvocations(arguments);
        Method method = methodInvocation.getMethod();
        String methodName = method.getName();
        String captorArguments = generateCaptorArguments(argumentNames, method);
        String fieldMock = methodInvocation.getFieldName();
        return bodySpace + MOCKITO_CLASS + ".verify(" + fieldMock + ", " + MOCKITO_CLASS + ".times(" + numberOfInvocation + "))." + methodName + "(" + captorArguments + ");\n";
    }

    private int getNumberOfInvocations(Map<String, List<Object>> arguments) {
        for (String argumentName : arguments.keySet()) {
            List<Object> passedParameters = arguments.get(argumentName);
            return passedParameters.size();
        }
        return 0;
    }

    protected String generateCaptorArguments(Set<String> argumentNames, Method method) {
        String methodName = method.getName();
        final String params = argumentNames.stream()
                .map(argumentName -> methodName + argumentName + CAPTOR_VARIABLE_SUFFIX + ".capture()")
                .reduce("", (acc, name) -> acc.concat(name + ","));
        return "".equals(params) ? "" : params.substring(0, params.length() - 1);
    }

    private String createArgumentCollection(MethodInvocation methodInvocation) {
        Map<String, List<Object>> arguments = methodInvocation.getArguments();
        Set<String> argumentNames = arguments.keySet();
        StringBuilder collectedArguments = new StringBuilder();
        for (String argumentName : argumentNames) {
            Method method = methodInvocation.getMethod();
            String methodName = method.getName();
            final String type = arguments.get(argumentName).get(0).getClass().getTypeName();
            String captor = methodName + argumentName + CAPTOR_VARIABLE_SUFFIX;
            String result = methodName + argumentName + CAPTOR_RESULT_VARIABLE_SUFFIX;
            String collectedArgument = bodySpace + "List<" + type + "> " + result + " = " + captor + ".getAllValues();\n";
            collectedArguments.append(collectedArgument);
        }
        return collectedArguments.toString();
    }

    private String createExpectedResults(MethodInvocation methodInvocation) {
        Map<String, List<Object>> arguments = methodInvocation.getArguments();
        Set<String> argumentNames = arguments.keySet();
        StringBuilder expectedResults = new StringBuilder();
        for (String argumentName : argumentNames) {
            Method method = methodInvocation.getMethod();
            String methodName = method.getName();
            List<Object> passedParameters = arguments.get(argumentName);
            String result = methodName + argumentName + CAPTOR_EXPECTED_RESULT_VARIABLE_SUFFIX;
            String expectedResult = bodySpace + "String " + result + " = \"" + passedParameters.toString() + "\";\n";
            expectedResults.append(expectedResult);
        }
        return expectedResults.toString();
    }

    private String createMockAssertions(MethodInvocation methodInvocation) {
        Map<String, List<Object>> arguments = methodInvocation.getArguments();
        Set<String> argumentNames = arguments.keySet();
        StringBuilder assertions = new StringBuilder();
        for (String argumentName : argumentNames) {
            Method method = methodInvocation.getMethod();
            String methodName = method.getName();
            String result = methodName + argumentName + CAPTOR_RESULT_VARIABLE_SUFFIX;
            String expectedResult = methodName + argumentName + CAPTOR_EXPECTED_RESULT_VARIABLE_SUFFIX;
            String assertion = bodySpace + ASSERT_CLASS + ".assertEquals(" + expectedResult + ", " + result + ".toString());\n";
            assertions.append(assertion);
        }
        return assertions.toString();
    }

    private void addNewInvocation(List<MethodInvocation> methodInvocations, MethodCapture methodCapture) {
        Method method = methodCapture.getMethod();
        MethodInvocation methodInvocation = new MethodInvocation(method, methodCapture.getFieldName());
        addInvocation(methodCapture, methodInvocation);
        methodInvocations.add(methodInvocation);
    }

    private void addToExistingInvocation(List<MethodInvocation> methodInvocations, MethodCapture methodCapture) {
        Method method = methodCapture.getMethod();
        methodInvocations.stream()
                .filter(methodInvocation -> methodInvocation.getMethod().equals(method))
                .findFirst()
                .ifPresent(methodInvocation -> this.addInvocation(methodCapture, methodInvocation));
    }

    private boolean isaMethodInvocationsContainMethod(List<MethodInvocation> methodInvocations, MethodCapture capture) {
        return methodInvocations.stream().anyMatch(methodInvocation -> methodInvocation.getMethod().equals(capture.getMethod()));
    }

    /**
     * Mutates MethodInvocation object!
     */
    private void addInvocation(MethodCapture capture, MethodInvocation methodInvocation) {
        Method method = capture.getMethod();
        List<String> parameterNames = getParameterNames(method);
        Object[] arguments = capture.getArguments();
        for (int index = 0; index < arguments.length; index++) {
            methodInvocation.addInvocation(parameterNames.get(index), arguments[index]);
        }
    }

    private List<MethodCapture> getDependenciesInvocations() {
        return capturedData.stream()
                .filter(capture -> capture.getType() == MethodCapture.Type.DEPENDENCY)
                .collect(Collectors.toList());
    }

    private List<MethodCapture> getAffectedDependenciesInvocations() {
        return capturedData.stream()
                .filter(capture -> capture.getType() == MethodCapture.Type.AFFECT)
                .collect(Collectors.toList());
    }
}
