/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.transformer;

import org.mule.api.transformer.TransformerException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transformer.AbstractTransformer;
import org.mule.config.i18n.MessageFactory;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import java.util.Map;
import java.util.Iterator;
import java.util.Collections;
import java.util.Arrays;

/**
 * Simple transformer for using the JAXP XPath library to extract an XPath value from
 * an XPath expression.
 * 
 * @author Ryan Heaton
 */
public class XPathExtractor extends AbstractTransformer
{

    /**
     * Result type.
     */
    public enum ResultType
    {
        NODESET,
        NODE,
        STRING,
        BOOLEAN,
        NUMBER
    }

    private volatile XPath xpath = XPathFactory.newInstance().newXPath();
    private volatile Map<String, String> prefixToNamespaceMap = null;
    private volatile String expression;
    private volatile ResultType resultType = ResultType.STRING;

    public XPathExtractor()
    {
        registerSourceType(org.w3c.dom.Node.class);
        registerSourceType(InputSource.class);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();

        if (expression == null)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("An expression must be supplied to the StandardXPathExtractor"),
                this);
        }

        final Map<String, String> prefixToNamespaceMap = this.prefixToNamespaceMap;
        if (prefixToNamespaceMap != null)
        {
            getXpath().setNamespaceContext(new NamespaceContext()
            {
                public String getNamespaceURI(String prefix)
                {
                    return prefixToNamespaceMap.get(prefix);
                }

                public String getPrefix(String namespaceURI)
                {

                    for (Map.Entry<String, String> entry : prefixToNamespaceMap.entrySet())
                    {
                        if (namespaceURI.equals(entry.getValue()))
                        {
                            return entry.getKey();
                        }
                    }

                    return null;
                }

                public Iterator getPrefixes(String namespaceURI)
                {
                    String prefix = getPrefix(namespaceURI);
                    if (prefix == null)
                    {
                        return Collections.emptyList().iterator();
                    }
                    else
                    {
                        return Arrays.asList(prefix).iterator();
                    }
                }
            });
        }
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        QName resultType;
        switch (getResultType())
        {
            case BOOLEAN :
                resultType = XPathConstants.BOOLEAN;
                break;
            case NODE :
                resultType = XPathConstants.NODE;
                break;
            case NODESET :
                resultType = XPathConstants.NODESET;
                break;
            case NUMBER :
                resultType = XPathConstants.NUMBER;
                break;
            default :
                resultType = XPathConstants.STRING;
                break;
        }

        try
        {
            if (src instanceof InputSource)
            {
                return xpath.evaluate(expression, (InputSource) src, resultType);
            }
            else
            {
                return xpath.evaluate(expression, src, resultType);
            }
        }
        catch (XPathExpressionException e)
        {
            throw new TransformerException(this, e);
        }
    }

    /**
     * @return Returns the expression.
     */
    public String getExpression()
    {
        return expression;
    }

    /**
     * @param expression The expression to set.
     */
    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    /**
     * Result type from this transformer.
     * 
     * @return Result type from this transformer.
     */
    public ResultType getResultType()
    {
        return resultType;
    }

    /**
     * Result type from this transformer.
     * 
     * @param resultType Result type from this transformer.
     */
    public void setResultType(ResultType resultType)
    {
        this.resultType = resultType;
    }

    /**
     * The XPath evaluator.
     * 
     * @return The XPath evaluator.
     */
    public XPath getXpath()
    {
        return xpath;
    }

    /**
     * The XPath evaluator.
     * 
     * @param xPath The XPath evaluator.
     */
    public void setXpath(XPath xPath)
    {
        this.xpath = xPath;
    }

    /**
     * The prefix-to-namespace map.
     * 
     * @return The prefix-to-namespace map.
     */
    public Map<String, String> getNamespaces()
    {
        return prefixToNamespaceMap;
    }

    /**
     * The prefix-to-namespace map.
     * 
     * @param prefixToNamespaceMap The prefix-to-namespace map.
     */
    public void setNamespaces(Map<String, String> prefixToNamespaceMap)
    {
        this.prefixToNamespaceMap = prefixToNamespaceMap;
    }
}
