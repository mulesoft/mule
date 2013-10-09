/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.expression;

import org.mule.api.annotations.expressions.Mule;
import org.mule.api.annotations.meta.Evaluator;
import org.mule.api.expression.ExpressionAnnotationParser;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.ExpressionArgument;

import java.lang.annotation.Annotation;

/**
 * Used to parse Mule parameter annotations
 *
 * @see org.mule.expression.MuleExpressionEvaluator
 * @see org.mule.api.annotations.expressions.Mule
 *
 * @since 3.0
 */
public class MuleAnnotationParser implements ExpressionAnnotationParser
{
    public ExpressionArgument parse(Annotation annotation, Class parameterType)
    {
        Evaluator evaluator = annotation.annotationType().getAnnotation(Evaluator.class);
        if (evaluator != null)
        {
            Mule muleAnnotation = (Mule)annotation;
            String val = muleAnnotation.value();

            if ("message.payload".equals(val))
            {
                //Match the param type and attempt to auto convert
                val += "(" + parameterType.getName() + ")";
            }

            return new ExpressionArgument(null, new ExpressionConfig(val, evaluator.value(), null),
                    muleAnnotation.optional(), parameterType);
        }
        else
        {
            throw new IllegalArgumentException("The @Evaluator annotation must be set on an Expression Annotation");
        }

    }

    public boolean supports(Annotation annotation)
    {
        return annotation instanceof Mule;
    }
}
