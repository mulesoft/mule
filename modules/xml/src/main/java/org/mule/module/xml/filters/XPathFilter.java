/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.filters;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.routing.filter.Filter;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.util.ClassUtils;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;

import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;

/**
 */
public class XPathFilter extends AbstractJaxpFilter  implements Filter, Initialisable, MuleContextAware
{
    protected transient Log logger = LogFactory.getLog(getClass());

    private String pattern;
    private String expectedValue;
    private XPath xpath;
    private Map<String, String> prefixToNamespaceMap = null;

    private NamespaceManager namespaceManager;

    private MuleContext muleContext;

    public XPathFilter()
    {
        super();
    }

    public XPathFilter(String pattern)
    {
        this.pattern = pattern;
    }

    public XPathFilter(String pattern, String expectedValue)
    {
        this.pattern = pattern;
        this.expectedValue = expectedValue;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();

        if (getXpath() == null)
        {
            setXpath(XPathFactory.newInstance().newXPath());
        }


        if (pattern == null)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("A pattern must be supplied to the " +
                                                   ClassUtils.getSimpleName(getClass())),
                this);
        }

        if (muleContext != null)
        {
            try
            {
                namespaceManager = muleContext.getRegistry().lookupObject(NamespaceManager.class);
            }
            catch (RegistrationException e)
            {
                throw new ExpressionRuntimeException(CoreMessages.failedToLoad("NamespaceManager"), e);
            }

            if (namespaceManager != null)
            {
                if (prefixToNamespaceMap == null)
                {
                    prefixToNamespaceMap = new HashMap<String, String>(namespaceManager.getNamespaces());
                }
                else
                {
                    prefixToNamespaceMap.putAll(namespaceManager.getNamespaces());
                }
            }
        }

        final Map<String, String> prefixToNamespaceMap = this.prefixToNamespaceMap;
        if (prefixToNamespaceMap != null)
        {
            getXpath().setNamespaceContext(new NamespaceContext()
            {
                @Override
                public String getNamespaceURI(String prefix)
                {
                    return prefixToNamespaceMap.get(prefix);
                }

                @Override
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

                @Override
                public Iterator<String> getPrefixes(String namespaceURI)
                {
                    String prefix = getPrefix(namespaceURI);
                    if (prefix == null)
                    {
                        return Collections.<String>emptyList().iterator();
                    }
                    else
                    {
                        return Arrays.asList(prefix).iterator();
                    }
                }
            });
        }

        if (logger.isInfoEnabled())
        {
            logger.info("XPath implementation: " + getXpath());
            logger.info("DocumentBuilderFactory implementation: " + getDocumentBuilderFactory());
        }
    }

    @Override
    public boolean accept(MuleMessage message)
    {
        Object payload = message.getPayload();
        if (payload == null)
        {
            if (logger.isWarnEnabled())
            {
                logger.warn("Applying " + ClassUtils.getSimpleName(getClass()) + " to null object.");
            }
            return false;
        }
        if (pattern == null)
        {
            if (logger.isWarnEnabled())
            {
                logger.warn("Expression for " + ClassUtils.getSimpleName(getClass()) + " is not set.");
            }
            return false;
        }
        if (expectedValue == null)
        {
            // Handle the special case where the expected value really is null.
            if (pattern.endsWith("= null") || pattern.endsWith("=null"))
            {
                expectedValue = "null";
                pattern = pattern.substring(0, pattern.lastIndexOf("="));
            }
            else
            {
                if (logger.isInfoEnabled())
                {
                    logger.info("''expectedValue'' attribute for " + ClassUtils.getSimpleName(getClass()) +
                                " is not set, using 'true' by default");
                }
                expectedValue = Boolean.TRUE.toString();
            }
        }

        Node node;
        try
        {
            node = toDOMNode(payload);
        }
        catch (Exception e)
        {
            if (logger.isWarnEnabled())
            {
                logger.warn(ClassUtils.getSimpleName(getClass()) + " filter rejected message because of an error while parsing XML: "
                            + e.getMessage(), e);
            }
            return false;
        }

        message.setPayload(node);

        return accept(node);
    }

    protected boolean accept(Node node)
    {
        Object xpathResult;
        boolean accept = false;

        try
        {
            xpathResult = getXpath().evaluate(pattern, node, XPathConstants.STRING);
        }
        catch (Exception e)
        {
            if (logger.isWarnEnabled())
            {
                logger.warn(
                        ClassUtils.getSimpleName(getClass()) + " filter rejected message because of an error while evaluating the expression: "
                        + e.getMessage(), e);
            }
            return false;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(MessageFormat.format("{0} Expression result = ''{1}'' -  Expected value = ''{2}''",
                                              ClassUtils.getSimpleName(getClass()), xpathResult, expectedValue));
        }

        // Compare the XPath result with the expected result.
        if (xpathResult != null && !"".equals(xpathResult))
        {
            accept = xpathResult.toString().equals(expectedValue);
        }
        else
        {
            // A null result was actually expected.
            if ("null".equals(expectedValue))
            {
                accept = true;
            }
            // A null result was not expected, something probably went wrong.
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(MessageFormat.format("{0} expression evaluates to null: {1}",
                                                      ClassUtils.getSimpleName(getClass()), pattern));
                }
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(MessageFormat.format("{0} accept object  : {1}", ClassUtils.getSimpleName(getClass()), accept));
        }

        return accept;
    }

    /**
     * @return XPath expression
     */
    public String getPattern()
    {
        return pattern;
    }

    /**
     * @param pattern The XPath expression
     */
    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    /**
     * @return The expected result value of the XPath expression
     */
    public String getExpectedValue()
    {
        return expectedValue;
    }

    /**
     * Sets the expected result value of the XPath expression
     *
     * @param expectedValue The expected value.
     */
    public void setExpectedValue(String expectedValue)
    {
        this.expectedValue = expectedValue;
    }

    /**
     * The xpath object to use to evaluate the expression.
     *
     * @return The xpath object to use to evaluate the expression.
     */
    public XPath getXpath()
    {
        return xpath;
    }

    /**
     * The xpath object to use to evaluate the expression.
     *
     * @param xpath The xpath object to use to evaluate the expression.
     */
    public void setXpath(XPath xpath)
    {
        this.xpath = xpath;
    }


    /**
     * The prefix-to-namespace map for the namespace context to be applied to the
     * XPath evaluation.
     *
     * @return The prefix-to-namespace map for the namespace context to be applied to
     *         the XPath evaluation.
     */
    public Map<String, String> getNamespaces()
    {
        return prefixToNamespaceMap;
    }

    /**
     * The prefix-to-namespace map for the namespace context to be applied to the
     * XPath evaluation.
     *
     * @param prefixToNamespaceMap The prefix-to-namespace map for the namespace
     *            context to be applied to the XPath evaluation.
     */
    public void setNamespaces(Map<String, String> prefixToNamespaceMap)
    {
        this.prefixToNamespaceMap = prefixToNamespaceMap;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final XPathFilter other = (XPathFilter) obj;
        return equal(expectedValue, other.expectedValue)
            && equal(prefixToNamespaceMap, other.prefixToNamespaceMap)
            && equal(pattern, other.pattern);
    }

    @Override
    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), expectedValue, prefixToNamespaceMap, pattern});
    }
}
