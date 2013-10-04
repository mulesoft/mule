/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.expressions;

import org.mule.api.annotations.meta.Evaluator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows method parameters to be configured with result of one or more Mule expressions
 *
 * public Object save(Object foo, @Expr("#[header:X-User]-#[function:UUID]") String id)
 *
 * @see org.mule.expression.FunctionExpressionEvaluator
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Evaluator("string")
public @interface Expr
{
    String value();

    boolean optional() default false;
}
