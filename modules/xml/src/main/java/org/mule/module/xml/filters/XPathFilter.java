/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.filters;

import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;
import org.mule.RequestContext;
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
import org.mule.module.xml.xpath.SaxonXpathEvaluator;
import org.mule.module.xml.xpath.XPathEvaluator;
import org.mule.util.ClassUtils;

import java.text.MessageFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public class XPathFilter extends AbstractJaxpFilter  implements Filter, Initialisable, MuleContextAware
{
    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    private String pattern;
    private String expectedValue;
    private XPathEvaluator xpathEvaluator;
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

        if (xpathEvaluator == null)
        {
            xpathEvaluator = new SaxonXpathEvaluator();
        }

        if (pattern == null)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("A pattern must be supplied to the " +
                                                   ClassUtils.getSimpleName(getClass())),
                this);
        }

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
            xpathEvaluator.registerNamespaces(namespaceManager);
        }

        if (prefixToNamespaceMap != null)
        {
            xpathEvaluator.registerNamespaces(prefixToNamespaceMap);
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
                logger.warn("Applying {} to null object.", ClassUtils.getSimpleName(getClass()));
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
                    logger.info("''expectedValue'' attribute for {} is not set, using 'true' by default", ClassUtils.getSimpleName(getClass()));
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
            xpathResult = xpathEvaluator.evaluate(pattern, node, RequestContext.getEvent());
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

    public void setXpathEvaluator(XPathEvaluator xpathEvaluator)
    {
        this.xpathEvaluator = xpathEvaluator;
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
