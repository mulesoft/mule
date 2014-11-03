/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.expression;

import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.module.xml.el.XPath3Function;
import org.mule.module.xml.i18n.XmlMessages;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.w3c.dom.Document;

/**
 * Will select the text of a single node based on the property name
 *
 * @deprecated This expression evaluator is deprecated and will be removed in Mule 4. Use {@link XPath3Function} instead
 */
@Deprecated
public class XPathNodeExpressionEvaluator extends XPathExpressionEvaluator
{
    public static final String NAME = "xpath-node";

    private DocumentBuilder builder;

    public XPathNodeExpressionEvaluator()
    {
        try
        {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException e)
        {
            throw new ExpressionRuntimeException(XmlMessages.failedToCreateDocumentBuilder(), e);
        }
    }

    @Override
    protected Object extractResultFromNode(Object result)
    {
        if (result instanceof Element)
        {
            ((Element) result).detach();
            return DocumentHelper.createDocument((Element) result);
        }
        else if (result instanceof org.w3c.dom.Element)
        {
            return extractW3CElement(result);
        }
        else
        {
            return result;
        }
    }

    protected Object extractW3CElement(Object result)
    {
        org.w3c.dom.Element element = (org.w3c.dom.Element) result;

        Document doc = builder.newDocument();
        doc.appendChild(doc.importNode(element, true));
        return doc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    protected String getDeprecationMessage()
    {
        return "The xpath-node: expression evaluator has been deprecated in Mule 3.6.0 and will be removed in 4.0. Please use the xpath3() MEL function instead";
    }
}
