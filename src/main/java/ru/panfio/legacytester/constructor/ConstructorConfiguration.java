package ru.panfio.legacytester.constructor;

import lombok.ToString;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;

@ToString
public class ConstructorConfiguration {
    private static final String DEFAULT_SPACE_BEFORE_METHOD_SIGNATURE = "    ";
    private static final String DEFAULT_SPACE_BEFORE_METHOD_BODY = "        ";
    private static final String ASSERT_CLASS = "org.junit.jupiter.api.Assertions";//"org.junit.Assert";
    private static final String TEST_ANNOTATION_CLASS = "org.junit.jupiter.api.Test";//"org.junit.Test";
    public static final String TEST_METHOD_NAME_SUFFIX = "Test";

    //todo create accessors
    //use builder
    public static final String MOCKITO_CLASS = "org.mockito.Mockito";
    public static final String ARGUMENT_CAPTOR_CLASS = "org.mockito.ArgumentCaptor";
    public static final String MOCK_FIELD_VARIABLE_SUFFIX = "Field";
    public static final String MOCK_PARAMETER_VARIABLE_SUFFIX = "PassedParameter";
    public static final String MOCK_RESULT_VARIABLE_SUFFIX = "ResultInvocation";
    public static final String CAPTOR_VARIABLE_SUFFIX = "Captor";
    public static final String CAPTOR_RESULT_VARIABLE_SUFFIX = "Result";
    public static final String CAPTOR_EXPECTED_RESULT_VARIABLE_SUFFIX = "ExpectedResult";

    private boolean isVerbose = true;
    private String assertionClass = ASSERT_CLASS;
    private String bodySpace = DEFAULT_SPACE_BEFORE_METHOD_BODY;
    private String signatureSpace = DEFAULT_SPACE_BEFORE_METHOD_SIGNATURE;
    private String testAnnotationClass = TEST_ANNOTATION_CLASS;
    private Function<Method, String> testMethodNameGenerator = (method) -> method.getName() + TEST_METHOD_NAME_SUFFIX + (int) (Math.random() * 100000);

    public ConstructorConfiguration verbose(boolean verbose) {
        this.isVerbose = verbose;
        return this;
    }

    public ConstructorConfiguration assertionClass(Class<?> assertionClass) {
        this.assertionClass = assertionClass.getTypeName();
        return this;
    }

    public String type(Class<?> target) {
        return isVerboseType(target.getTypeName());
    }

    public String type(Parameter parameter) {
        return isVerboseType(parameter.getParameterizedType().getTypeName());
    }

    public String type(Method method) {
        Type type = method.getGenericReturnType();
        if (type instanceof ParameterizedType) {
            return isVerboseType(type.getTypeName());
        } else {
            return type(type.getClass());
        }
    }

    public String assertionClass() {
        return isVerboseType(assertionClass);
    }

    public String mockitoClass() {
        return isVerboseType(MOCKITO_CLASS);
    }

    public String captorClass() {
        return isVerboseType(MOCKITO_CLASS);
    }

    private String isVerboseType(String type) {
        return isVerbose ? type : unVerbose(type);
    }

    public ConstructorConfiguration bodySpace(String bodySpace) {
        this.bodySpace = bodySpace;
        return this;
    }

    public String bodySpace() {
        return bodySpace;
    }

    public ConstructorConfiguration signatureSpace(String signatureSpace) {
        this.signatureSpace = signatureSpace;
        return this;
    }

    public String signatureSpace() {
        return signatureSpace;
    }

    public Function<Method, String> testMethodNameGenerator() {
        return testMethodNameGenerator;
    }

    public ConstructorConfiguration testMethodNameGenerator(Function<Method, String> testMethodNameGenerator) {
        this.testMethodNameGenerator = testMethodNameGenerator;
        return this;
    }

    public ConstructorConfiguration testAnnotationClass(Class<?> testAnnotationClass) {
        this.testAnnotationClass = testAnnotationClass.getTypeName();
        return this;
    }

    public String testAnnotationClass() {
        return assertionClass;
    }

    private String unVerbose(String typeName) {
        String result = "";
        boolean isVerbose = false;
        for (int i = typeName.length() - 1; i > 0; i--) {
            char c = typeName.charAt(i);
            if (c == '.') isVerbose = true;
            if (c == '<' || c == '>' || c == ' ' || c == '?') isVerbose = false;
            if (!isVerbose) result += typeName.charAt(i);
        }
        return new StringBuilder(result).reverse().toString();
    }
}
