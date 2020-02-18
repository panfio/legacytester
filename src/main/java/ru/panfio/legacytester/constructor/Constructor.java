package ru.panfio.legacytester.constructor;

import com.fasterxml.jackson.core.type.TypeReference;
import ru.panfio.legacytester.MethodCapture;
import ru.panfio.legacytester.util.JsonUtils;
import ru.panfio.legacytester.util.SerializableUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    private final ConstructorConfiguration conf;
    private final MethodCapture testMethodCapture;

    public Constructor(Class<?> testClass,
                       ConstructorConfiguration conf,
                       MethodCapture testMethodCapture) {
        this.testClass = testClass;
        this.conf = conf;
        this.testMethodCapture = testMethodCapture;
    }

    protected static boolean isIsaSerializableType(String type) {
        return TYPES_FOR_SERIALIZATION.stream().anyMatch(type::equals);
    }

    protected String generateThrowsDeclaration() {
        return " throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, java.lang.reflect.InvocationTargetException ";
    }

    protected String generateTestAnnotation() {
        return conf.getSignatureSpace() + "@Test\n";
    }

    protected String generateTestMethodName() {
        String methodName = conf.getTestMethodNameGenerator().apply(testMethodCapture.getMethod());
        return conf.getSignatureSpace() + "public void " + methodName + "()" + generateThrowsDeclaration() + "{\n";
    }

    protected String generateCloseBracket() {
        return conf.getSignatureSpace() + "}";
    }

    protected String generateClassCreation() {
        final String constructorArguments = repeatArguments(getConstructorArguments(testClass), "null");
        return conf.getBodySpace() + "//Please create a test class manually if necessary\n" +
                conf.getBodySpace() + conf.type(testClass) + " testClass = new " + conf.type(testClass) + "(" + constructorArguments + ");\n";
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
            String param = generateObjectSerialization(params[index], name, conf.type(parameter));
            inputData.append(param);
        }
        return inputData.toString();
    }

    protected String generateObjectSerialization(Object value, String name, String type) {
        if (isIsaSerializableType(type) && Serializable.class.isAssignableFrom(value.getClass())) {
            String serVal = serializeToString((Serializable) value);
            return conf.getBodySpace() + "//Original value: " + commentLineBreaks(value.toString()) + "\n" +
                    conf.getBodySpace() + type + " " + name + " = (" + type + ") " + conf.type(SerializableUtils.class) + ".serializeFromString(\"" + serVal + "\");\n";
        }
        final String jsonValue = toJson(value);
        return conf.getBodySpace() + type + " " + name + " = " + conf.type(JsonUtils.class) + ".parse(\"" + escapeQuotes(jsonValue) + "\", new " + conf.type(TypeReference.class) + "<" + type + ">() {});\n";
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
        String returnType = conf.type(testMethod);
        String params = generateArguments(testMethod);
        if (testMethodCapture.getException() != null) {
            String exceptionType = testMethodCapture.getException().getClass().getTypeName();
            return conf.getBodySpace() + conf.getAssertion() + ".assertThrows(" + exceptionType + ".class, () -> testClass." + testMethodCapture.getMethod().getName() + "(" + params + "));\n";
        }
        if (isVoidReturnType()) {
            return conf.getBodySpace() + "testClass." + methodName + "(" + params + ");\n";
        }
        return conf.getBodySpace() + returnType + " result = testClass." + methodName + "(" + params + ");\n";
    }

    private String privateMethodInvocation() {
        Method testMethod = testMethodCapture.getMethod();
        String methodName = testMethod.getName();
        String returnType = conf.type(testMethod);
        String params = generateArguments(testMethod);
        final String parameterClasses = generateParameterClasses(testMethod);
        String methodConfiguration = setMethodAccessible(methodName, parameterClasses);
        if (testMethodCapture.getException() != null) {
            String exceptionType = testMethodCapture.getException().getClass().getTypeName();
            return methodConfiguration + conf.getBodySpace() + conf.getAssertion() + ".assertThrows(" + exceptionType + ".class, () -> {try{" + testMethodCapture.getMethod().getName() + ".invoke(testClass, " + params + ");} catch (" + conf.type(InvocationTargetException.class)+ " e) {throw e.getCause();}});\n";
        }
        if (isVoidReturnType()) {
            return conf.getBodySpace() + "testClass." + methodName + "(" + params + ");\n";
        }
        return methodConfiguration + invokePrivateMethod(returnType + " result = (" + returnType + ") ", methodName, params);
    }

    private String setMethodAccessible(String methodName, String parameterClasses) {
        return conf.getBodySpace() + conf.type(Method.class) + " " + methodName + " = testClass.getClass().getDeclaredMethod(\"" + methodName + "\", " + parameterClasses + ");\n" +
                conf.getBodySpace() + methodName + ".setAccessible(true);\n";

    }

    private String invokePrivateMethod(String result, String methodName, String params) {
        return conf.getBodySpace() + result + methodName + ".invoke(testClass, " + params + ");\n";
    }

    private String generateParameterClasses(Method method) {
        final List<Parameter> parameterTypes = getMethodParameters(method);
        final String params = parameterTypes.stream()
                .map(conf::type)
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
            return conf.getBodySpace() + "String expectedResult = " + null + ";\n";
        } else {
            return conf.getBodySpace() + "String expectedResult = \"" + escapeQuotes(expectedResult.toString()) + "\";\n";
        }
    }

    private boolean isVoidReturnType() {
        return "void".equals(testMethodCapture.getMethod().getGenericReturnType().getTypeName());
    }

    protected String generateAssertions() {
        final Throwable exception = testMethodCapture.getException();
        if (exception == null) {
            return generateResultAssertToString();
        }
        return "";
    }

    protected String generateResultAssertToString() {
        if (isVoidReturnType()) {
            return "";
        }
        return conf.getBodySpace() + conf.getAssertion() + ".assertEquals(expectedResult, result.toString());\n";
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
