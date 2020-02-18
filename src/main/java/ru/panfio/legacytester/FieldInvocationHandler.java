package ru.panfio.legacytester;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        //todo catch mock exceptions
        final String methodName = method.getName();
        if (affectedMethods.contains(methodName)) {
            capturedInvocations.add(
                    MethodCapture.builder()
                            .method(method)
                            .type(MethodCapture.Type.AFFECT)
                            .arguments(args)
                            .result(result)
                            .exception(null)
                            .build());
            return result;
        }
        capturedInvocations.add(
                MethodCapture.builder()
                        .method(method)
                        .type(MethodCapture.Type.DEPENDENCY)
                        .arguments(args)
                        .result(result)
                        .exception(null)
                        .build());
        return result;
    }
}
