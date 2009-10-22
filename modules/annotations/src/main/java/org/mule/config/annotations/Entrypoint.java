package org.mule.config.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a serviceMethod as being discoverable. This means that methods with this annotation can be invoked by
 * an external client or service.
 *
 * The entrypoint method may have further annotations to describe how each of the parameters are to be extracted
 * from the current message. If there are no further annotations, Mule will use other entrypoint resolvers to work
 * out how to match the entrypoint parameters with the data held in the current event.
 *
 * @see org.mule.config.annotations.expressions.XPath
 * @see org.mule.config.annotations.expressions.Mule
 * @see org.mule.config.annotations.expressions.Groovy
 * @see org.mule.config.annotations.expressions.Bean
 * @see org.mule.config.annotations.expressions.ExpressionString
 * @see org.mule.config.annotations.expressions.Ognl
 * @see org.mule.config.annotations.expressions.Function
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Entrypoint
{
}
