package ru.panfio.legacytester;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Container for storing method invocation data.
 */
public class MethodCapture {
    public enum Type { DEPENDENCY, AFFECT, TEST }
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
