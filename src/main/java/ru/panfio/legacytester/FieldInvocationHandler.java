package ru.panfio.legacytester;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;

public class FieldInvocationHandler implements InvocationHandler {
    private final Object target;
    //todo find more efficient solution to check for what field is proxy created
    private String fieldName;

    private final List<String> affectedMethods;
    private List<MethodCapture> capturedInvocations = new ArrayList<>();
    public FieldInvocationHandler(Object target,
                                  String... affectedMethods) {
        this.target = target;
        this.affectedMethods = Arrays.asList(affectedMethods);
    }

    public String getFieldName() {
        return fieldName;
    }

    public FieldInvocationHandler setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public List<MethodCapture> getCapturedInvocations() {
        return capturedInvocations;
    }

    public void clearCapturedInvocations() {
        this.capturedInvocations = new ArrayList<>();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(target, args);

        final String methodName = method.getName();
        if (affectedMethods.contains(methodName)) {
            capturedInvocations.add(new MethodCapture(method, MethodCapture.Type.AFFECT, args, result));
            return result;
        }

        capturedInvocations.add(new MethodCapture(method, MethodCapture.Type.DEPENDENCY, args, result));
        return result;
    }
}
