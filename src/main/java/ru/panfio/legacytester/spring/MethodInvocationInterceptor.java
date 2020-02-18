package ru.panfio.legacytester.spring;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import ru.panfio.legacytester.LegacyTester;
import ru.panfio.legacytester.Testee;

import java.lang.reflect.Method;

public class MethodInvocationInterceptor implements MethodInterceptor {

    private final Object target;
    private final LegacyTester legacyTester;

    public MethodInvocationInterceptor(Object target, LegacyTester legacyTester) {
        this.target = target;
        this.legacyTester = legacyTester;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] params, MethodProxy methodProxy) throws Throwable {
        if (method.isAnnotationPresent(Testee.class)) {
            Testee testeeAnnotation = method.getAnnotation(Testee.class);
            if (testeeAnnotation.qualifier().equals(legacyTester.getQualifier())) {
                legacyTester.clearInvocations();
                return legacyTester.test(() -> method.invoke(target, params), params);
            }
        }
        return method.invoke(target, params);
    }
}
