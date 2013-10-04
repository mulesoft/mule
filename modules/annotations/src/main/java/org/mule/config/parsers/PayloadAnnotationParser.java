/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.parsers;

import org.mule.api.annotations.meta.Evaluator;
import org.mule.api.annotations.param.Payload;
import org.mule.api.expression.ExpressionAnnotationParser;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.ExpressionArgument;

import java.lang.annotation.Annotation;

/**
 * Responsible for parsing the {@link org.mule.api.annotations.param.Payload} annotation.  This is an iBeans
 * framework class and cannot be used in any other context.
 */
public class PayloadAnnotationParser implements ExpressionAnnotationParser
{
    public ExpressionArgument parse(Annotation annotation, Class<?> parameterType)
    {
        Evaluator evaluator = annotation.annotationType().getAnnotation(Evaluator.class);
        if (evaluator != null)
        {
            return new ExpressionArgument(null, new ExpressionConfig(parameterType.getName(), evaluator.value(), null), false, parameterType);
        }
        else
        {
            throw new IllegalArgumentException("The @Evaluator annotation must be set on an Expression Annotation");
        }
    }

    public boolean supports(Annotation annotation)
    {
        return annotation instanceof Payload;
    }
}
