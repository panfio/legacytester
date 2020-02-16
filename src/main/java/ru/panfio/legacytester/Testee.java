package ru.panfio.legacytester;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking testable class and methods.
 */
@Target(value = {
        ElementType.METHOD,
        ElementType.TYPE,
        ElementType.FIELD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Testee {
    /**
     * Connects annotated method with concrete tester instance.
     */
    String qualifier() default "default";

    /**
     * Contain methods for capturing dependency invocations.
     */
    String[] affectedMethods() default "";
}