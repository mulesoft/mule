/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.expression;

import org.mule.module.xml.i18n.XmlMessages;

import org.dom4j.Node;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.jaxen.dom4j.Dom4jXPath;

/**
 * Will select the text of a single node based on the property name
 */
public class XPathExpressionEvaluator extends AbstractXPathExpressionEvaluator
{
    public static final String NAME = "xpath";

    @Override
    protected XPath createXPath(String expression, Object object) throws JaxenException
    {
        if(createDOMXPath(object))
        {
            return new DOMXPath(expression);
        }
        else if (createDom4jXPath(object))
        {
            return new Dom4jXPath(expression);
        }
        else
        {
            throw new IllegalArgumentException(XmlMessages.domTypeNotSupported(object.getClass()).getMessage());
        }
    }

    @Override
    protected String getXPathClassName(Object object)
    {
        if(createDOMXPath(object))
        {
            return DOMXPath.class.getName();
        }
        if(createDom4jXPath(object))
        {
            return Dom4jXPath.class.getName();
        }
        return super.getXPathClassName(object);
    }

    private boolean createDOMXPath(Object object)
    {
        return object instanceof org.w3c.dom.Document || object instanceof org.w3c.dom.Element;
    }

    private boolean createDom4jXPath(Object object)
    {
        return object instanceof org.dom4j.Document || object instanceof org.dom4j.Element;
    }

    protected Object extractResultFromNode(Object result)
    {
        if(result instanceof Node)
        {
            return ((Node)result).getText();
        }
        else if(result instanceof org.w3c.dom.Node)
        {
            org.w3c.dom.Node firstChild = ((org.w3c.dom.Node) result).getFirstChild();
            return firstChild == null ? "" : firstChild.getNodeValue();
        }
        else
        {
            return result;
        }
    }

    /** {@inheritDoc} */
    public String getName()
    {
        return NAME;
    }
}
