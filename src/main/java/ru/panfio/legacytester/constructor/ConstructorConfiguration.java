package ru.panfio.legacytester.constructor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;

public class ConstructorConfiguration {
    private final boolean isVerbose;
    private final String assertion;
    private final String argumentCaptor;
    private final String bodySpace;
    private final String captorVariableSuffix;
    private final String captorExpectedResultVariableSuffix;
    private final String captorResultVariableSuffix;
    private final String mockito;
    private final String mockFieldVariableSuffix;
    private final String mockParameterVariableSuffix;
    private final String mockResultVariableSuffix;
    private final String signatureSpace;
    private final String testAnnotation;
    private final Function<Method, String> testMethodNameGenerator;

    ConstructorConfiguration(boolean isVerbose, String assertion, String argumentCaptor, String bodySpace,
                             String aptorVariableSuffix, String aptorExpectedResultVariableSuffix,
                             String aptorResultVariableSuffix, String mockito, String ockFieldVariableSuffix,
                             String ockParameterVariableSuffix, String ockResultVariableSuffix,
                             String signatureSpace, String testAnnotation, Function<Method, String> testMethodNameGenerator) {
        this.isVerbose = isVerbose;
        this.assertion = assertion;
        this.argumentCaptor = argumentCaptor;
        this.bodySpace = bodySpace;
        this.captorVariableSuffix = aptorVariableSuffix;
        this.captorExpectedResultVariableSuffix = aptorExpectedResultVariableSuffix;
        this.captorResultVariableSuffix = aptorResultVariableSuffix;
        this.mockito = mockito;
        this.mockFieldVariableSuffix = ockFieldVariableSuffix;
        this.mockParameterVariableSuffix = ockParameterVariableSuffix;
        this.mockResultVariableSuffix = ockResultVariableSuffix;
        this.signatureSpace = signatureSpace;
        this.testAnnotation = testAnnotation;
        this.testMethodNameGenerator = testMethodNameGenerator;
    }

    public static ConstructorConfigurationBuilder builder() {
        return new ConstructorConfigurationBuilder();
    }

    private String isVerboseType(String type) {
        return isVerbose ? type : unVerbose(type);
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

    public String getArgumentCaptor() {
        return isVerboseType(argumentCaptor);
    }

    public String getCaptorVariableSuffix() {
        return captorVariableSuffix;
    }

    public String getCaptorExpectedResultVariableSuffix() {
        return captorExpectedResultVariableSuffix;
    }

    public String getCaptorResultVariableSuffix() {
        return captorResultVariableSuffix;
    }

    public String getMockFieldVariableSuffix() {
        return mockFieldVariableSuffix;
    }

    public String getMockParameterVariableSuffix() {
        return mockParameterVariableSuffix;
    }

    public String getMockResultVariableSuffix() {
        return mockResultVariableSuffix;
    }

    public String getTestAnnotation() {
        return testAnnotation;
    }

    public String getAssertion() {
        return isVerboseType(assertion);
    }

    public String getMockito() {
        return isVerboseType(mockito);
    }

    public String getBodySpace() {
        return bodySpace;
    }

    public String getSignatureSpace() {
        return signatureSpace;
    }

    public Function<Method, String> getTestMethodNameGenerator() {
        return testMethodNameGenerator;
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

    public static class ConstructorConfigurationBuilder {
        private static final String DEFAULT_SPACE_BEFORE_METHOD_SIGNATURE = "    ";
        private static final String DEFAULT_SPACE_BEFORE_METHOD_BODY = "        ";
        private static final String ASSERT_CLASS = "org.junit.jupiter.api.Assertions";
        private static final String TEST_ANNOTATION_CLASS = "org.junit.jupiter.api.Test";
        private static final String TEST_METHOD_NAME_SUFFIX = "Test";
        private static final String MOCKITO_CLASS = "org.mockito.Mockito";
        private static final String ARGUMENT_CAPTOR_CLASS = "org.mockito.ArgumentCaptor";
        private static final String MOCK_FIELD_VARIABLE_SUFFIX = "Field";
        private static final String MOCK_PARAMETER_VARIABLE_SUFFIX = "PassedParameter";
        private static final String MOCK_RESULT_VARIABLE_SUFFIX = "ResultInvocation";
        private static final String CAPTOR_VARIABLE_SUFFIX = "Captor";
        private static final String CAPTOR_RESULT_VARIABLE_SUFFIX = "Result";
        private static final String CAPTOR_EXPECTED_RESULT_VARIABLE_SUFFIX = "ExpectedResult";
        private static final Function<Method, String> TEST_METHOD_NAME_GENERATOR = (method) -> method.getName() + TEST_METHOD_NAME_SUFFIX + (int) (Math.random() * 100000);

        private boolean isVerbose = false;
        private String assertion = ASSERT_CLASS;
        private String argumentCaptor = ARGUMENT_CAPTOR_CLASS;
        private String bodySpace = DEFAULT_SPACE_BEFORE_METHOD_BODY;
        private String captorVariableSuffix = CAPTOR_VARIABLE_SUFFIX;
        private String captorExpectedResultVariableSuffix = CAPTOR_EXPECTED_RESULT_VARIABLE_SUFFIX;
        private String captorResultVariableSuffix = CAPTOR_RESULT_VARIABLE_SUFFIX;
        private String mockito = MOCKITO_CLASS;
        private String mockFieldVariableSuffix = MOCK_FIELD_VARIABLE_SUFFIX;
        private String mockParameterVariableSuffix = MOCK_PARAMETER_VARIABLE_SUFFIX;
        private String mockResultVariableSuffix = MOCK_RESULT_VARIABLE_SUFFIX;
        private String signatureSpace = DEFAULT_SPACE_BEFORE_METHOD_SIGNATURE;
        private String testAnnotation = TEST_ANNOTATION_CLASS;
        private Function<Method, String> testMethodNameGenerator = TEST_METHOD_NAME_GENERATOR;

        ConstructorConfigurationBuilder() {
        }

        public ConstructorConfiguration build() {
            return new ConstructorConfiguration(isVerbose, assertion, argumentCaptor, bodySpace,
                    captorVariableSuffix, captorExpectedResultVariableSuffix,
                    captorResultVariableSuffix, mockito, mockFieldVariableSuffix,
                    mockParameterVariableSuffix, mockResultVariableSuffix,
                    signatureSpace, testAnnotation, testMethodNameGenerator);
        }

        public String toString() {
            return "ConstructorConfiguration.ConstructorConfigurationBuilder()";
        }

        public ConstructorConfigurationBuilder verbose(boolean isVerbose) {
            this.isVerbose = isVerbose;
            return this;
        }

        public ConstructorConfigurationBuilder assertion(String assertion) {
            this.assertion = assertion;
            return this;
        }

        public ConstructorConfigurationBuilder argumentCaptor(String argumentCaptor) {
            this.argumentCaptor = argumentCaptor;
            return this;
        }

        public ConstructorConfigurationBuilder bodySpace(String bodySpace) {
            this.bodySpace = bodySpace;
            return this;
        }

        public ConstructorConfigurationBuilder captorVariableSuffix(String aptorVariableSuffix) {
            this.captorVariableSuffix = aptorVariableSuffix;
            return this;
        }

        public ConstructorConfigurationBuilder captorExpectedResultVariableSuffix(String aptorExpectedResultVariableSuffix) {
            this.captorExpectedResultVariableSuffix = aptorExpectedResultVariableSuffix;
            return this;
        }

        public ConstructorConfigurationBuilder captorResultVariableSuffix(String aptorResultVariableSuffix) {
            this.captorResultVariableSuffix = aptorResultVariableSuffix;
            return this;
        }

        public ConstructorConfigurationBuilder mockito(String mockito) {
            this.mockito = mockito;
            return this;
        }

        public ConstructorConfigurationBuilder mockFieldVariableSuffix(String ockFieldVariableSuffix) {
            this.mockFieldVariableSuffix = ockFieldVariableSuffix;
            return this;
        }

        public ConstructorConfigurationBuilder mockParameterVariableSuffix(String ockParameterVariableSuffix) {
            this.mockParameterVariableSuffix = ockParameterVariableSuffix;
            return this;
        }

        public ConstructorConfigurationBuilder mockResultVariableSuffix(String ockResultVariableSuffix) {
            this.mockResultVariableSuffix = ockResultVariableSuffix;
            return this;
        }

        public ConstructorConfigurationBuilder signatureSpace(String signatureSpace) {
            this.signatureSpace = signatureSpace;
            return this;
        }

        public ConstructorConfigurationBuilder testAnnotation(String testAnnotation) {
            this.testAnnotation = testAnnotation;
            return this;
        }

        public ConstructorConfigurationBuilder testMethodNameGenerator(Function<Method, String> testMethodNameGenerator) {
            this.testMethodNameGenerator = testMethodNameGenerator;
            return this;
        }

        public ConstructorConfigurationBuilder testAnnotationClass(Class<?> testAnnotationClass) {
            this.testAnnotation = testAnnotationClass.getTypeName();
            return this;
        }

        public ConstructorConfigurationBuilder assertionClass(Class<?> assertionClass) {
            this.assertion = assertionClass.getTypeName();
            return this;
        }
    }
}
