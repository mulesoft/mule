package org.mule.config.annotations.expressions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows method parameters to be configured as bean expressions for example:
 *
 * public Object doSomething(@Bean("person.age") int age)
 *
 * @see org.mule.module.xml.expression.BeanPayloadExpressionEvaluator
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Evaluator("bean")
public @interface Bean
{
    String value();

    boolean required() default true;
}
