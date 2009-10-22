/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.expression.parsers;

import org.mule.api.expression.ExpressionParser;
import org.mule.config.annotations.expressions.CustomEvaluator;
import org.mule.config.annotations.expressions.Evaluator;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.ExpressionArgument;

import java.lang.annotation.Annotation;

/**
 * Used to parse custom expressions annotations
 *
 * @see org.mule.config.annotations.expressions.CustomEvaluator
 * @see org.mule.expression.CustomExpressionEvaluator
 */
public class CustomEvaluatorAnnotationParser implements ExpressionParser
{
    public ExpressionArgument parse(Annotation annotation, Class parameterType)
    {
        Evaluator evaluator = annotation.annotationType().getAnnotation(Evaluator.class);
        if (evaluator != null)
        {
            CustomEvaluator eval = (CustomEvaluator) annotation;
            ExpressionArgument arg = new ExpressionArgument(null, new ExpressionConfig(eval.value(), evaluator.value(), eval.evaluator()), ((CustomEvaluator) annotation).required(), parameterType);
            return arg;
        }
        else
        {
            throw new IllegalArgumentException("The @Evaluator annotation must be set on an Expression Annotation");
        }

    }

    public boolean supports(Annotation annotation)
    {
        return annotation instanceof CustomEvaluator;
    }
}