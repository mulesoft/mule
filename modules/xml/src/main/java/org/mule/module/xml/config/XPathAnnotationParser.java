/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.config;

import org.mule.api.annotations.expression.XPath;
import org.mule.api.annotations.meta.Evaluator;
import org.mule.api.expression.ExpressionAnnotationParser;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.module.xml.i18n.XmlMessages;

import java.lang.annotation.Annotation;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Used to parse Bean parameter annotations. Note this annotation only supports the Jaxp API and w3c Dom.  There is
 * not Dom4J support.
 *
 * @see org.mule.api.annotations.expression.XPath
 * @see org.mule.module.xml.expression.XPathExpressionEvaluator
 */
public class XPathAnnotationParser implements ExpressionAnnotationParser
{
    public ExpressionArgument parse(Annotation annotation, Class<?> parameterType)
    {
        Evaluator evaluator = annotation.annotationType().getAnnotation(Evaluator.class);
        String eval = "xpath2";
        String type;
        if (evaluator != null)
        {
            if (parameterType.equals(Node.class) || parameterType.equals(org.dom4j.Node.class) ||
                    parameterType.equals(Element.class) || parameterType.equals(org.dom4j.Element.class) ||
                    parameterType.equals(Document.class) || parameterType.equals(org.dom4j.Document.class))
            {
                type = "[node]";
            }
            else if(NodeList.class.isAssignableFrom(parameterType))
            {
                type = "[nodeset]";
            }
            else if(Boolean.class.isAssignableFrom(parameterType))
            {
                type = "[boolean]";
            }
            else if(Double.class.isAssignableFrom(parameterType))
            {
                type = "[number]";
            }
            else if(String.class.isAssignableFrom(parameterType))
            {
                type = "[string]";
            }
            else
            {
                throw new IllegalArgumentException(XmlMessages.xpathResultTypeNotSupported(parameterType).getMessage());
            }
            return new ExpressionArgument(null, new ExpressionConfig(type + ((XPath) annotation).value(),
                    eval, null), ((XPath) annotation).optional(), parameterType);
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
