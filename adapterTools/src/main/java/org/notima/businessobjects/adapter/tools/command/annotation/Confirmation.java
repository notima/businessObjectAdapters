package org.notima.businessobjects.adapter.tools.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.notima.businessobjects.adapter.tools.command.annotation.processor.BasicConfirmationProcessor;

/**
 * Use this annotation to register a Confirmation class to use
 * for an {@link org.apache.karaf.shell.api.action.Option} or 
 * {@link org.apache.karaf.shell.api.action.Argument} when executing 
 * a command.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Confirmation {
    Class<?> type() default BasicConfirmationProcessor.class;
    String messageFormat() default "%s is %s";
}
