package ru.panfio.legacytester;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static ru.panfio.legacytester.util.ReflectionUtils.getParameterNames;

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

    public String methodName() {
        return method.getName();
    }

    public Object getArgument(String argumentName) {
        return arguments.get(argumentName);
    }

    public Class argumentType(String argumentName) {
        return arguments.get(argumentName).get(0).getClass();
    }

    public String getFieldName() {
        return fieldName;
    }

    public Map<String, List<Object>> getArguments() {
        return arguments;
    }

    public Set<String> argumentNames() {
        return this.getArguments().keySet();
    }

    public String forEachArgument(BiFunction<MethodInvocation, String, String> biFunction) {
        StringBuilder builder = new StringBuilder();
        for (String argumentName : this.argumentNames()) {
            builder.append(biFunction.apply(this, argumentName));
        }
        return builder.toString();
    }

    public String apply(Function<MethodInvocation, String> function) {
        return function.apply(this);
    }

    public int invocationCount() {
        for (String argumentName : arguments.keySet()) {
            List<Object> passedParameters = arguments.get(argumentName);
            return passedParameters.size();
        }
        return 0;
    }

    public static List<MethodInvocation> of(List<MethodCapture> affected) {
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

    private static void addNewInvocation(List<MethodInvocation> methodInvocations,
                                         MethodCapture methodCapture) {
        Method method = methodCapture.getMethod();
        MethodInvocation methodInvocation = new MethodInvocation(method, methodCapture.getFieldName());
        addInvocation(methodCapture, methodInvocation);
        methodInvocations.add(methodInvocation);
    }

    private static void addToExistingInvocation(List<MethodInvocation> methodInvocations,
                                                MethodCapture methodCapture) {
        Method method = methodCapture.getMethod();
        methodInvocations.stream()
                .filter(methodInvocation -> methodInvocation.getMethod().equals(method))
                .findFirst()
                .ifPresent(methodInvocation -> addInvocation(methodCapture, methodInvocation));
    }

    private static boolean isaMethodInvocationsContainMethod(List<MethodInvocation> methodInvocations,
                                                             MethodCapture capture) {
        return methodInvocations.stream()
                .anyMatch(methodInvocation -> methodInvocation.getMethod().equals(capture.getMethod()));
    }

    /**
     * Mutates MethodInvocation object!
     */
    private static void addInvocation(MethodCapture capture, MethodInvocation methodInvocation) {
        Method method = capture.getMethod();
        List<String> parameterNames = getParameterNames(method);
        Object[] arguments = capture.getArguments();
        for (int index = 0; index < arguments.length; index++) {
            methodInvocation.addInvocation(parameterNames.get(index), arguments[index]);
        }
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
