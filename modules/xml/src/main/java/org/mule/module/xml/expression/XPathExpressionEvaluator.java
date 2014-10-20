/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.expression;

import org.mule.module.xml.el.XPath3Function;
import org.mule.module.xml.i18n.XmlMessages;

import org.dom4j.Node;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.jaxen.dom4j.Dom4jXPath;

/**
 * Will select the text of a single node based on the property name
 *
 * @deprecated This expression evaluator is deprecated and will be removed in Mule 4. Use {@link XPath3Function} instead
 */
@Deprecated
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

    @Override
    protected String getDeprecationMessage()
    {
        return "The xpath: expression evaluator has been deprecated in Mule 3.6.0 and will be removed in 4.0. Please use the xpath3() MEL function instead";
    }
}
