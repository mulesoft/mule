/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
