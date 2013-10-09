/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.expression;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;

import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.javabean.JavaBeanXPath;

/** TODO */
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
}
