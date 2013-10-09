/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.expression;

import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.module.xml.i18n.XmlMessages;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.w3c.dom.Document;

/**
 * Will select the text of a single node based on the property name
 */
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
}
