/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.parsers;

import org.mule.api.annotations.meta.Evaluator;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.expression.ExpressionAnnotationParser;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.MessageHeaderExpressionEvaluator;
import org.mule.expression.MessageHeadersExpressionEvaluator;
import org.mule.expression.MessageHeadersListExpressionEvaluator;
import org.mule.expression.transformers.ExpressionArgument;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import static org.mule.expression.ExpressionConstants.OPTIONAL_ARGUMENT;

/**
 * Responsible for parsing the {@link org.mule.api.annotations.param.InboundHeaders} annotation.  This is an iBeans
 * framework class and cannot be used in any other context.
 */
public class InboundHeadersAnnotationParser implements ExpressionAnnotationParser
{
    public ExpressionArgument parse(Annotation annotation, Class<?> parameterType)
    {
        Evaluator evaluator = annotation.annotationType().getAnnotation(Evaluator.class);

        if (evaluator != null)
        {
            String expr = ((InboundHeaders) annotation).value();

            boolean optional = false;
            String eval = MessageHeaderExpressionEvaluator.NAME;
            if (parameterType.isAssignableFrom(Map.class))
            {
                eval = MessageHeadersExpressionEvaluator.NAME;
            }
            else if (parameterType.isAssignableFrom(List.class))
            {
                eval = MessageHeadersListExpressionEvaluator.NAME;
            }
            else if (expr.endsWith(OPTIONAL_ARGUMENT))
            {
                //We only set optional if we're dealing with a single header, List and Maps of headers can contain
                //optional names but we will always return a Map or List even if it is empty
                optional = true;
            }
            return new ExpressionArgument(null, new ExpressionConfig("INBOUND:" + expr, eval, null), optional, parameterType);
        }
        else
        {
            throw new IllegalArgumentException("The @Evaluator annotation must be set on an Expression Annotation");
        }
    }

    public boolean supports(Annotation annotation)
    {
        return annotation instanceof InboundHeaders;
    }
}
