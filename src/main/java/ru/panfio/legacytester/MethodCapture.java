package ru.panfio.legacytester;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Container for storing method invocation data.
 */
public class MethodCapture {
    public enum Type {DEPENDENCY, AFFECT, TEST}

    private final Method method;
    private final Type type;
    private final Object[] arguments;
    private final Object result;

    private String fieldName;

    public MethodCapture(Method method, Type type, Object[] arguments, Object result) {
        this.method = method;
        this.type = type;
        this.arguments = arguments;
        this.result = result;
    }

    public Method getMethod() {
        return method;
    }

    public String methodName() {
        return method.getName();
    }

    public Type getType() {
        return type;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public Object getResult() {
        return result;
    }

    public String getFieldName() {
        return fieldName;
    }

    public MethodCapture setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }


    public static List<MethodCapture> dependenciesInvocations(List<MethodCapture> capturedData) {
        return capturedData.stream()
                .filter(capture -> capture.getType() == Type.DEPENDENCY)
                .collect(Collectors.toList());
    }

    public static List<MethodCapture> affectedInvocations(List<MethodCapture> capturedData) {
        return capturedData.stream()
                .filter(capture -> capture.getType() == Type.AFFECT)
                .collect(Collectors.toList());
    }

    public static MethodCapture testInvocation(List<MethodCapture> capturedData) {
        return capturedData.stream()
                .filter(capture -> capture.getType() == Type.TEST)
                .findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return "MethodCapture{" +
                "method=" + method +
                ", type=" + type +
                ", arguments=" + Arrays.toString(arguments) +
                ", result=" + result +
                ", fieldName='" + fieldName + '\'' +
                '}';
    }
}
