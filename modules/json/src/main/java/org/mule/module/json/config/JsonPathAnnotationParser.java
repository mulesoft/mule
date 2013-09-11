/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.config;

import org.mule.api.annotations.expression.JsonPath;
import org.mule.api.annotations.meta.Evaluator;
import org.mule.api.expression.ExpressionAnnotationParser;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.ExpressionArgument;

import java.lang.annotation.Annotation;

/**
 * Responsible for parsing {@link org.mule.api.annotations.expression.JsonPath} parameter annotations
 */
public class JsonPathAnnotationParser implements ExpressionAnnotationParser
{
    public ExpressionArgument parse(Annotation annotation, Class<?> parameterType)
    {
        Evaluator evaluator = annotation.annotationType().getAnnotation(Evaluator.class);

        return new ExpressionArgument(null, new ExpressionConfig(((JsonPath) annotation).value(),
                evaluator.value(), null), ((JsonPath) annotation).optional(), parameterType);
    }

    public boolean supports(Annotation annotation)
    {
        return annotation instanceof JsonPath;
    }
}
