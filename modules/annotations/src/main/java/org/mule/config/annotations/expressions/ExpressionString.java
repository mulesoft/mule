package org.mule.config.annotations.expressions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows method parameters to be configured with functions
 *
 * public Object save(Object foo, @Function("UUID") String id)
 *
 * @see org.mule.expression.FunctionExpressionEvaluator
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Evaluator("string")
public @interface ExpressionString
{
    public abstract String value();

    public abstract boolean required() default true;
}