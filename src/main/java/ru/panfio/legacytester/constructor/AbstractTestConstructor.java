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
public abstract class AbstractTestConstructor {
    private final static String DEFAULT_SPACE_BEFORE_METHOD_SIGNATURE = "    ";
    private static final String DEFAULT_SPACE_BEFORE_METHOD_BODY = "        ";
    private static final String TEST_METHOD_NAME_SUFFIX = "Test";
    protected static final String ASSERT_CLASS = "org.junit.jupiter.api.Assertions";//"org.junit.Assert";
    private static final List<String> TYPES_FOR_SERIALIZATION = new ArrayList<>(Arrays.asList("java.lang.String",
            "byte", "short", "int", "long", "float", "double", "boolean", "char",
            "byte[]", "short[]", "int[]", "long[]", "float[]", "double[]", "boolean[]", "char[]"));
    protected Class<?> testClass;
    protected MethodCapture testableMethodCapture;
    protected String signatureSpace = DEFAULT_SPACE_BEFORE_METHOD_SIGNATURE;
    protected String bodySpace = DEFAULT_SPACE_BEFORE_METHOD_BODY;

    public AbstractTestConstructor(Class<?> testClass, MethodCapture testableMethodCapture) {
        this.testClass = testClass;
        this.testableMethodCapture = testableMethodCapture;
    }

    public AbstractTestConstructor spaceBeforeSignature(String signatureSpace) {
        this.signatureSpace = signatureSpace;
        return this;
    }

    public AbstractTestConstructor spaceBeforeBody(String bodySpace) {
        this.bodySpace = bodySpace;
        return this;
    }

    protected static boolean isIsaSerializableType(String type) {
        return TYPES_FOR_SERIALIZATION.stream().anyMatch(type::equals);
    }

    protected String generateThrowsDeclaration() {
        return " throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, java.lang.reflect.InvocationTargetException ";
    }

    protected String generateTestAnnotation() {
        return signatureSpace + "@Test\n";
    }

    protected String generateTestMethodName(Method testMethod) {
        String originalMethodName = testMethod.getName();
        int id = (int) (Math.random()*100000);
        return signatureSpace + "public void " + originalMethodName + TEST_METHOD_NAME_SUFFIX + id + "()" + generateThrowsDeclaration() + "{\n";
    }

    protected String generateCloseBracket() {
        return signatureSpace + "}";
    }

    protected String generateClassCreation(Class testClass) {
        final String className = testClass.getTypeName();
        final String constructorArguments = repeatArguments(getConstructorArguments(testClass), "null");
        return bodySpace + "//Please create a test class manually if necessary\n" +
                bodySpace + className + " testClass = new " + className + "(" + constructorArguments + ");\n";
    }

    protected String generateInputParams(Method testMethod, Object[] params) {
        List<Parameter> parameters = getMethodParameters(testMethod);
        if (parameters.isEmpty()) {
            return "";
        }
        if (parameters.size() != params.length) {
            throw new RuntimeException("Parameter mismatch. Please pass all parameters");
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
            return bodySpace + "//Original value: " + commentLineBreaks(value.toString()) + "\n" +
                    bodySpace + type + " " + name + " = (" + type + ") ru.panfio.legacytester.util.SerializableUtils.serializeFromString(\"" + serVal + "\");\n";
        }
        final String jsonValue = toJson(value);
        return bodySpace + type + " " + name + " = ru.panfio.legacytester.util.JsonUtils.parse(\"" + escapeQuotes(jsonValue) + "\", new com.fasterxml.jackson.core.type.TypeReference<" + type + ">() {});\n";
    }


    protected String generateParameterClasses(Method method) {
        final List<String> parameterTypes = getParameterTypes(method);
        final String params = parameterTypes.stream()
                .map(argumentType -> argumentType + ".class")
                .reduce("", (acc, name) -> acc.concat(name + ","));
        return "".equals(params) ? "" : params.substring(0, params.length() - 1);
    }

    // TODO refactor
    protected String generateTestMethodInvocation(Method testMethod) {
        String methodName = testMethod.getName();
        String returnType = getmethodReturnType(testMethod);
        String params = generateArguments(testMethod);
        final String parameterClasses = generateParameterClasses(testMethod);
        if ("void".equals(returnType)) {
            if (Modifier.isPrivate(testMethod.getModifiers())) {
                return bodySpace + "java.lang.reflect.Method method = testClass.getClass().getDeclaredMethod(\"" + methodName + "\", " + parameterClasses + ");\n" +
                        bodySpace + "method.setAccessible(true);\n" +
                        bodySpace + "method.invoke(testClass, " + params + ");\n";
            }
            return bodySpace + "testClass." + methodName + "(" + params + ");\n";
        }
        if (Modifier.isPrivate(testMethod.getModifiers())) {
            return bodySpace + "java.lang.reflect.Method method = testClass.getClass().getDeclaredMethod(\"" + methodName + "\", " + parameterClasses + ");\n" +
                    bodySpace + "method.setAccessible(true);\n" +
                    bodySpace + returnType + " result = (" + returnType + ") method.invoke(testClass, " + params + ");\n";
        }
        return bodySpace + returnType + " result = testClass." + methodName + "(" + params + ");\n";
    }

    protected String generateResultToString(Object expectedResult) {
        if (isVoidReturnType()) {
            return "";
        }
        if (expectedResult == null) {
            return bodySpace + "String expectedResult = " + null + ";\n";
        } else {
            return bodySpace + "String expectedResult = \"" + escapeQuotes(expectedResult.toString()) + "\";\n";
        }
    }

    private boolean isVoidReturnType() {
        return "void".equals(testableMethodCapture.getMethod().getGenericReturnType().getTypeName());
    }

    protected String generateResultAssertToString() {
        if (isVoidReturnType()) {
            return "";
        }
        return bodySpace + ASSERT_CLASS + ".assertEquals(expectedResult, result.toString());\n";
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
