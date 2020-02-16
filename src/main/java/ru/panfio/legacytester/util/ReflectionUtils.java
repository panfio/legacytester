package ru.panfio.legacytester.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ReflectionUtils {
    private ReflectionUtils() {
        throw new RuntimeException("Utility class");
    }

    public static List<Parameter> getMethodParameters(Method method) {
        Parameter[] params = method.getParameters();
        return new ArrayList<>(Arrays.asList(params));
    }

    public static List<Field> getClassFields(Class targetClass) {
        Field[] declaredFields = targetClass.getDeclaredFields();
        return new ArrayList<>(Arrays.asList(declaredFields));
    }

    public static List<String> getParameterTypes(Method method) {
        List<String> parameters = new ArrayList<>();
        Type[] gpType = method.getGenericParameterTypes();
        for (int i = 0; i < gpType.length; i++) {
            parameters.add(gpType[i].getTypeName());
        }
        return parameters;
    }

    public static List<String> getParameterNames(Method method) {
        List<String> parameters = new ArrayList<>();
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            parameters.add(params[i].getName());
        }
        return parameters;
    }

    public static String getmethodReturnType(Method method) {
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
            return returnType.getTypeName();
        } else {
            return method.getReturnType().getTypeName();
        }
    }

    /**
     * Returns first declared constructor argument count
     */
    public static int getConstructorArguments(Class<?> targetClass) {
        for (Constructor constructor : targetClass.getDeclaredConstructors()) {
            return constructor.getGenericParameterTypes().length;
        }
        return 0;
    }

    public static boolean containAnnotation(Method method, Class<? extends Annotation> annotation) {
        boolean isContain = false;
        if (method.getAnnotation(annotation) != null) {
            isContain = true;
        }
        return isContain;
    }
}
