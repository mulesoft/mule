/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.api.annotations.expression.XPath;
import org.mule.api.annotations.meta.Evaluator;
import org.mule.api.expression.ExpressionAnnotationParser;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.ExpressionArgument;

import java.lang.annotation.Annotation;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Used to parse Bean parameter annotations
 *
 * @see org.mule.api.annotations.expression.XPath
 * @see org.mule.module.xml.expression.XPathExpressionEvaluator
 */
public class XPathAnnotationParser implements ExpressionAnnotationParser
{
    public ExpressionArgument parse(Annotation annotation, Class<?> parameterType)
    {
        Evaluator evaluator = annotation.annotationType().getAnnotation(Evaluator.class);
        String eval = "xpath";
        if (evaluator != null)
        {
            if (parameterType.equals(Node.class) || parameterType.equals(org.dom4j.Node.class) ||
                    parameterType.equals(Element.class) || parameterType.equals(org.dom4j.Element.class) ||
                    parameterType.equals(Document.class) || parameterType.equals(org.dom4j.Document.class))
            {
                eval = "xpath-node";
            }
            ExpressionArgument arg = new ExpressionArgument(null, new ExpressionConfig(((XPath) annotation).value(),
                    eval, null), ((XPath) annotation).required(), parameterType);
            return arg;
        }
        else
        {
            throw new IllegalArgumentException("The @Evaluator annotation must be set on an Expression Annotation");
        }
    }

    public boolean supports(Annotation annotation)
    {
        return annotation instanceof XPath;
    }
}
