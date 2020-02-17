package ru.panfio.legacytester.constructor;

import lombok.ToString;

//@Builder
@ToString
public class ConstructorConfiguration {
    private static final String DEFAULT_SPACE_BEFORE_METHOD_SIGNATURE = "    ";
    private static final String DEFAULT_SPACE_BEFORE_METHOD_BODY = "        ";
    private static final String ASSERT_CLASS = "org.junit.jupiter.api.Assertions";//"org.junit.Assert";

    //todo create accessors
    public static final String TEST_METHOD_NAME_SUFFIX = "Test";
    public static final String MOCKITO_CLASS = "org.mockito.Mockito";
    public static final String ARGUMENT_CAPTOR_CLASS = "org.mockito.ArgumentCaptor";
    public static final String MOCK_FIELD_VARIABLE_SUFFIX = "Field";
    public static final String MOCK_PARAMETER_VARIABLE_SUFFIX = "PassedParameter";
    public static final String MOCK_RESULT_VARIABLE_SUFFIX = "ResultInvocation";
    public static final String CAPTOR_VARIABLE_SUFFIX = "Captor";
    public static final String CAPTOR_RESULT_VARIABLE_SUFFIX = "Result";
    public static final String CAPTOR_EXPECTED_RESULT_VARIABLE_SUFFIX = "ExpectedResult";

    private String signatureSpace = DEFAULT_SPACE_BEFORE_METHOD_SIGNATURE;
    private String bodySpace = DEFAULT_SPACE_BEFORE_METHOD_BODY;
    private String assertionClass = ASSERT_CLASS;


    public String assertionClass() {
        return assertionClass;
    }

    public ConstructorConfiguration assertionClass(String assertionClass) {
        this.assertionClass = assertionClass;
        return this;
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
}
