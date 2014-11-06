/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.expression;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;

import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.javabean.JavaBeanXPath;

/**
 * @deprecated This expression evaluator is deprecated and will be removed in Mule 4.0. Use MEL expressions instead
 */
@Deprecated
public class BeanPayloadExpressionEvaluator extends AbstractXPathExpressionEvaluator
{
    public static final String NAME = "bean";

    protected XPath createXPath(String expression, Object object) throws JaxenException
    {
        expression = expression.replaceAll("[.]", "/");
        return new JavaBeanXPath(expression);
    }

    protected Object extractResultFromNode(Object result)
    {
        if (result instanceof org.jaxen.javabean.Element)
        {
            return ((org.jaxen.javabean.Element) result).getObject();
        }
        return result;
    }

    @Override
    protected Object getPayloadForXPath(MuleMessage message) throws TransformerException
    {
        return message.getPayload();
    }

    /** {@inheritDoc} */
    public String getName()
    {
        return NAME;
    }

    @Override
    protected String getDeprecationMessage()
    {
        return "The bean: expression evaluator has been deprecated in Mule 3.6.0 and will be removed in 4.0. Please use MEL expressions instead";
    }
}
