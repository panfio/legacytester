package ru.panfio.legacytester.constructor;

import ru.panfio.legacytester.MethodCapture;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;

import static ru.panfio.legacytester.util.JsonUtils.toJson;
import static ru.panfio.legacytester.util.ReflectionUtils.*;
import static ru.panfio.legacytester.util.SerializableUtils.serializeToString;

/**
 * Class contains common functionality for all Test Constructors.
 */
public class Constructor {
    private static final List<String> TYPES_FOR_SERIALIZATION = new ArrayList<>(Arrays.asList("java.lang.String",
            "byte", "short", "int", "long", "float", "double", "boolean", "char",
            "byte[]", "short[]", "int[]", "long[]", "float[]", "double[]", "boolean[]", "char[]"));
    private final Class<?> testClass;
    private final MethodCapture testMethodCapture;
    private ConstructorConfiguration conf = new ConstructorConfiguration();

    public Constructor(Class<?> testClass, MethodCapture testMethodCapture) {
        this.testClass = testClass;
        this.testMethodCapture = testMethodCapture;
    }

    public ConstructorConfiguration configuration() {
        return conf;
    }

    public Constructor configuration(ConstructorConfiguration conf) {
        this.conf = conf;
        return this;
    }

    protected static boolean isIsaSerializableType(String type) {
        return TYPES_FOR_SERIALIZATION.stream().anyMatch(type::equals);
    }

    protected String generateThrowsDeclaration() {
        return " throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, java.lang.reflect.InvocationTargetException ";
    }

    protected String generateTestAnnotation() {
        return conf.signatureSpace() + "@Test\n";
    }

    protected String generateTestMethodName() {
        String originalMethodName = testMethodCapture.methodName();
        int id = (int) (Math.random() * 100000);
        return conf.signatureSpace() + "public void " + originalMethodName + conf.TEST_METHOD_NAME_SUFFIX + id + "()" + generateThrowsDeclaration() + "{\n";
    }

    protected String generateCloseBracket() {
        return conf.signatureSpace() + "}";
    }

    protected String generateClassCreation() {
        final String className = testClass.getTypeName();
        final String constructorArguments = repeatArguments(getConstructorArguments(testClass), "null");
        return conf.bodySpace() + "//Please create a test class manually if necessary\n" +
                conf.bodySpace() + className + " testClass = new " + className + "(" + constructorArguments + ");\n";
    }

    protected String generateInputParams() {
        Object[] params = testMethodCapture.getArguments();
        List<Parameter> parameters = getMethodParameters(testMethodCapture.getMethod());
        if (parameters.isEmpty()) {
            return "";
        }

        StringBuilder inputData = new StringBuilder();
        for (int index = 0; index < params.length; index++) {
            Parameter parameter = parameters.get(index);
            final String name = parameter.getName();
            final String type = parameter.getParameterizedType().getTypeName();
            String param = generateObjectSerialization(params[index], name, type);
            inputData.append(param);
        }
        return inputData.toString();
    }

    protected String generateObjectSerialization(Object value, String name, String type) {
        if (isIsaSerializableType(type) && Serializable.class.isAssignableFrom(value.getClass())) {
            String serVal = serializeToString((Serializable) value);
            return conf.bodySpace() + "//Original value: " + commentLineBreaks(value.toString()) + "\n" +
                    conf.bodySpace() + type + " " + name + " = (" + type + ") ru.panfio.legacytester.util.SerializableUtils.serializeFromString(\"" + serVal + "\");\n";
        }
        final String jsonValue = toJson(value);
        return conf.bodySpace() + type + " " + name + " = ru.panfio.legacytester.util.JsonUtils.parse(\"" + escapeQuotes(jsonValue) + "\", new com.fasterxml.jackson.core.type.TypeReference<" + type + ">() {});\n";
    }


    protected String generateTestMethodInvocation() {
        if (Modifier.isPrivate(testMethodCapture.getMethod().getModifiers())) {
            return privateMethodInvocation();
        }
        return publicMethodInvocation();
    }

    private String publicMethodInvocation() {
        Method testMethod = testMethodCapture.getMethod();
        String methodName = testMethod.getName();
        String returnType = getmethodReturnType(testMethod);
        String params = generateArguments(testMethod);
        if (testMethodCapture.getException() != null) {
            String exceptionType = testMethodCapture.getException().getClass().getTypeName();
            return conf.bodySpace() + conf.assertionClass() + ".assertThrows(" + exceptionType + ".class, () -> testClass." + testMethodCapture.getMethod().getName() + "(" + params + "));\n";
        }
        if ("void".equals(returnType)) {
            return conf.bodySpace() + "testClass." + methodName + "(" + params + ");\n";
        }
        return conf.bodySpace() + returnType + " result = testClass." + methodName + "(" + params + ");\n";
    }

    private String privateMethodInvocation() {
        Method testMethod = testMethodCapture.getMethod();
        String methodName = testMethod.getName();
        String returnType = getmethodReturnType(testMethod);
        String params = generateArguments(testMethod);
        final String parameterClasses = generateParameterClasses(testMethod);
        String methodConfiguration = setMethodAccessible(methodName, parameterClasses);
        if (testMethodCapture.getException() != null) {
            String exceptionType = testMethodCapture.getException().getClass().getTypeName();
            return methodConfiguration + conf.bodySpace() + conf.assertionClass() + ".assertThrows(" + exceptionType + ".class, () -> {try{" + testMethodCapture.getMethod().getName() + ".invoke(testClass, " + params + ");} catch (java.lang.reflect.InvocationTargetException e) {throw e.getCause();}});\n";
        }
        if ("void".equals(returnType)) {
            return conf.bodySpace() + "testClass." + methodName + "(" + params + ");\n";
        }
        return methodConfiguration + invokePrivateMethod(returnType + " result = (" + returnType + ") ", methodName, params);
    }

    private String setMethodAccessible(String methodName, String parameterClasses) {
        return conf.bodySpace() + "java.lang.reflect.Method " + methodName + " = testClass.getClass().getDeclaredMethod(\"" + methodName + "\", " + parameterClasses + ");\n" +
                conf.bodySpace() + methodName + ".setAccessible(true);\n";

    }

    private String invokePrivateMethod(String result, String methodName, String params) {
        return conf.bodySpace() + result + methodName + ".invoke(testClass, " + params + ");\n";
    }

    private String generateParameterClasses(Method method) {
        final List<String> parameterTypes = getParameterTypes(method);
        final String params = parameterTypes.stream()
                .map(argumentType -> argumentType + ".class")
                .reduce("", (acc, name) -> acc.concat(name + ","));
        return "".equals(params) ? "" : params.substring(0, params.length() - 1);
    }

    protected String generateResultToString() {
        if (testMethodCapture.getException() != null) {
            return "";
        }
        Object expectedResult = testMethodCapture.getResult();
        if (isVoidReturnType()) {
            return "";
        }
        if (expectedResult == null) {
            return conf.bodySpace() + "String expectedResult = " + null + ";\n";
        } else {
            return conf.bodySpace() + "String expectedResult = \"" + escapeQuotes(expectedResult.toString()) + "\";\n";
        }
    }

    private boolean isVoidReturnType() {
        return "void".equals(testMethodCapture.getMethod().getGenericReturnType().getTypeName());
    }

    protected String generateAssertions() {
        final Throwable exception = testMethodCapture.getException();
        if (exception != null) {
            return generateResultAssertToString();
        }
        return "";
    }

    protected String generateResultAssertToString() {
        if (isVoidReturnType()) {
            return "";
        }
        return conf.bodySpace() + conf.assertionClass() + ".assertEquals(expectedResult, result.toString());\n";
    }

    protected static String repeatArguments(int count, String argumentName) {
        final String params =
                Collections.nCopies(count, argumentName)
                        .stream()
                        .reduce("", (acc, name) -> acc.concat(name + ","));
        return "".equals(params) ? "" : params.substring(0, params.length() - 1);
    }

    protected String generateArguments(Method testMethod) {
        List<String> parameterNames = getParameterNames(testMethod);
        final String params = parameterNames.stream().reduce("", (acc, name) -> acc.concat(name + ","));
        return "".equals(params) ? "" : params.substring(0, params.length() - 1);
    }

    public String escapeQuotes(String text) {
        return text.replace("\"", "\\\"");
    }

    public String commentLineBreaks(String text) {
        return text.replace("\n", "").replace("\r", "");
    }
}
