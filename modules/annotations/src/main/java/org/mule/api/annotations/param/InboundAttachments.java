package org.mule.api.annotations.param;

import org.mule.api.annotations.meta.Evaluator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on methods that have a {@link org.mule.ibeans.api.application.Receive} or {@link org.mule.ibeans.api.application.ReceiveAndReply}
 * channel annotation, this annotation marks the method parameter that will be used to pass in one or more of the headers received.
 * This annotation can define a single header, a comma-separated list of header names, or '*' to denote all headers. By default,
 * if a named header is not present, an exception will be thrown. However, if the header name is defined with the '*' postfix, it
 * will be marked as optional.
 * <p/>
 * When defining multiple header names or using the '*' wildcard, the parameter can be a {@link java.util.Map} or {@link java.util.List}. If a
 * Map is used, the header name and value is passed in. If List is used, just the header value is used. If a single header name is
 * defined, the header type can be used as the parameter or List or Map can be used too.
 *
 * The Inbound attachments collection is immutable, so the attachments Map or List passed in will be immutable too. Attempting to write to the Map or List will result in an {@link UnsupportedOperationException}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Evaluator("attachments")
//
public @interface InboundAttachments
{
    /**
     * Defines the headers that should be injected into the parameter. This can be a single header, a comma-separated
     * list of header names, or '*' to denote all headers. By default, if a named header is not present, an exception will
     * be thrown. However, if the header name is defined with the '*' postfix, it will be marked as optional.
     *
     * @return
     */
    public abstract String value();
}