package ru.panfio.legacytester.spring;

import ru.panfio.legacytester.FieldInvocationHandler;
import ru.panfio.legacytester.LegacyTester;
import ru.panfio.legacytester.Testee;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class LegacyTesterBeanPostProcessor {

    private final Object bean;
    private final String beanName;
    private final Class<?> beanClass;

    public LegacyTesterBeanPostProcessor(Object bean, String beanName) {
        this.bean = bean;
        this.beanName = beanName;
        this.beanClass = bean.getClass();
    }

    public Object createProxy() {
        if (!beanClass.isAnnotationPresent(Testee.class)) {
            return bean;
        }

        LegacyTester tester = new LegacyTester(beanClass);
        final Field[] declaredFields = beanClass.getDeclaredFields();
        for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Class<?> fieldClass = field.getType();
            String fieldName = field.getName();
            field.setAccessible(true);
            try {
                Object target = field.get(bean);
                String[] affectedMethods = extractAffectedMethods(field);
                if (affectedMethods == null || affectedMethods.length == 0) {
                    // TODO replace with cglib proxy
                    field.set(bean, tester.createFieldProxy(fieldClass, new FieldInvocationHandler(target).setFieldName(fieldName), fieldClass));
                } else {
                    field.set(bean, tester.createFieldProxy(fieldClass, new FieldInvocationHandler(target, affectedMethods).setFieldName(fieldName), fieldClass));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return LegacyTester.createClassProxy(bean, tester);
    }

    private String[] extractAffectedMethods(Field field) {
        String[] affectedMethods = null;
        if (field.isAnnotationPresent(Testee.class)) {
            Testee fieldAnnotation = field.getAnnotation(Testee.class);
            affectedMethods = fieldAnnotation.affectedMethods();
        }
        return affectedMethods;
    }
}
