package io.github.opencubicchunks.javaheaders.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks that a class must be replaced by the class in {@code value}
 * <p>
 * If the target is a class (or abstract class), use a class (or abstract class) <br>
 * If the target is an interface, use an interface
 * <p>
 * {@link RetentionPolicy#RUNTIME} to make things easier to debug, it's not actually necessary
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DeclaresClass {
    /**
     * @return Class to replace it with in the form {@code the.package.name.ClassName}
     */
    String value();
}
