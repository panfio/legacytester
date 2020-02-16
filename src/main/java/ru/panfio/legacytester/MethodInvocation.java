package ru.panfio.legacytester;

import java.lang.reflect.Method;
import java.util.*;

public class MethodInvocation {
    private final Method method;
    private final String fieldName;
    Map<String, List<Object>> arguments = new LinkedHashMap<>();

    public MethodInvocation(Method method, String field) {
        this.method = method;
        this.fieldName = field;
    }

    public void addInvocation(String parameterName, Object value) {
        if (arguments.containsKey(parameterName)) {
            arguments.get(parameterName).add(value);
        } else {
            arguments.put(parameterName, new ArrayList<>(Collections.singletonList(value)));
        }
    }

    public Method getMethod() {
        return method;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Map<String, List<Object>> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return "MethodInvocation{" +
                "method=" + method +
                ", field='" + fieldName + '\'' +
                ", arguments=" + arguments +
                '}';
    }
}
