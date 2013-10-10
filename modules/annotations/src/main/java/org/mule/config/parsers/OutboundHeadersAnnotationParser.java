/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.parsers;

import org.mule.api.annotations.meta.Evaluator;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.api.expression.ExpressionAnnotationParser;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.ExpressionArgument;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Responsible for parsing the {@link org.mule.api.annotations.param.OutboundHeaders} annotation.  This is an iBeans
 * framework class and cannot be used in any other context.
 */
public class OutboundHeadersAnnotationParser implements ExpressionAnnotationParser
{
    public ExpressionArgument parse(Annotation annotation, Class<?> parameterType)
    {
        if(!Map.class.isAssignableFrom(parameterType))
        {
            throw new IllegalArgumentException("The @OutboundHeaders annotation can only be set on a java.util.Map parameter");
        }
        Evaluator evaluator = annotation.annotationType().getAnnotation(Evaluator.class);
        if (evaluator != null)
        {
            return new ExpressionArgument(null, new ExpressionConfig("", evaluator.value(), null), false);
        }
        else
        {
            throw new IllegalArgumentException("The @Evaluator annotation must be set on an Expression Annotation");
        }
    }

    public boolean supports(Annotation annotation)
    {
        return annotation instanceof OutboundHeaders;
    }
}
