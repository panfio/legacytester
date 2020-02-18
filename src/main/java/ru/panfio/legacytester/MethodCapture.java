package ru.panfio.legacytester;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Container for storing method invocation data.
 */
public class MethodCapture {
    public static MethodCaptureBuilder builder() {
        return new MethodCaptureBuilder();
    }

    public enum Type {DEPENDENCY, AFFECT, TEST}

    private final Method method;
    private final Type type;
    private final Object[] arguments;
    private final Object result;
    private final Throwable exception;

    private String fieldName;

    public MethodCapture(Method method, Type type, Object[] arguments,
                         Object result, Throwable exception) {
        this.method = method;
        this.type = type;
        this.arguments = arguments;
        this.result = result;
        this.exception = exception;
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

    public Throwable getException() {
        return exception;
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

    public static List<MethodCapture> collectFrom(Map<String, FieldInvocationHandler> handlers) {
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

    public static class MethodCaptureBuilder {
        private Method method;
        private Type type;
        private Object[] arguments;
        private Object result;
        private Throwable exception;

        MethodCaptureBuilder() {
        }

        public MethodCaptureBuilder method(Method method) {
            this.method = method;
            return this;
        }

        public MethodCaptureBuilder type(Type type) {
            this.type = type;
            return this;
        }

        public MethodCaptureBuilder arguments(Object[] arguments) {
            this.arguments = arguments;
            return this;
        }

        public MethodCaptureBuilder result(Object result) {
            this.result = result;
            return this;
        }

        public MethodCaptureBuilder exception(Throwable exception) {
            this.exception = exception;
            return this;
        }

        public MethodCapture build() {
            return new MethodCapture(method, type, arguments, result, exception);
        }

        public String toString() {
            return "MethodCapture.MethodCaptureBuilder(method=" + this.method +
                    ", type=" + this.type +
                    ", arguments=" + Arrays.deepToString(this.arguments) +
                    ", result=" + this.result +
                    ", exception=" + this.exception + ")";
        }
    }
}
