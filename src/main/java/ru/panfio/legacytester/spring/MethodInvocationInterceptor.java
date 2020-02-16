package ru.panfio.legacytester.spring;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import ru.panfio.legacytester.LegacyTester;
import ru.panfio.legacytester.Testee;

import java.lang.reflect.Method;

import static ru.panfio.legacytester.util.ReflectionUtils.getmethodReturnType;

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
                String returnType = getmethodReturnType(method);
                if ("void".equals(returnType)) {
                    legacyTester.clearInvocations();
                    Object result = method.invoke(target, params);
                    legacyTester.test(params);
                    return result;
                } else {
                    legacyTester.clearInvocations();
                    Object result = method.invoke(target, params);
                    legacyTester.test(result, params);
                    return result;
                }
            }
        }
        return method.invoke(target, params);
    }
}
