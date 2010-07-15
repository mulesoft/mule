package org.mule.api.annotations.params;

import org.mule.api.annotations.meta.Evaluator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on methods that have a {@link org.mule.ibeans.api.application.Receive} or {@link org.mule.ibeans.api.application.ReceiveAndReply}
 * channel annotation, this annotation marks the method parameter that will be used to pass in the message payload. Note that
 * the parameter type will be used to do any auto conversions using transformers available inside the iBeans container. iBeans has
 * a number of standard transformers for dealing with common Java types such as XML documents, streams, byte arrays, strings, etc.
 * It is also very easy for users to define their own using the {@link org.mule.ibeans.api.application.Transformer} annotation.
 *
 * @see org.mule.ibeans.api.application.Transformer
 * @see org.mule.ibeans.api.application.Receive
 * @see org.mule.ibeans.api.application.ReceiveAndReply
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Evaluator("payload")
public @interface MessagePayload
{
}