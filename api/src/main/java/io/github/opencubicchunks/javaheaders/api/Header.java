package io.github.opencubicchunks.javaheaders.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks that a class or inteface is a Java Header to be implemented.
 * <p>
 * {@link RetentionPolicy#RUNTIME} to make things easier to debug, it's not actually necessary
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Header {
}
