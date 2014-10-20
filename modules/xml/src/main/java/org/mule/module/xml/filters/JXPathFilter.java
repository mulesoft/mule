/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.filters;

import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.routing.filter.Filter;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.module.xml.util.XMLUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.jxpath.AbstractFactory;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;

/**
 * <code>JXPathFilter</code> evaluates an XPath expression against a W3C Document,
 * XML string, or Java bean and returns true if the result is as expected.
 *
 * @deprecated This feature is deprecated and will be removed in Mule 4.0. Use an expression-filter
 * for filtering based in a Java Object or the xpath-filter in the case of an XML document
 */
@Deprecated
public class JXPathFilter implements Filter, MuleContextAware, Initialisable
{
    protected transient Log logger = LogFactory.getLog(getClass());

    private String pattern;
    private String expectedValue;
    private Map<String, String> namespaces = null;
    private Map contextProperties = null;
    private AbstractFactory factory;
    private boolean lenient = true;

    private MuleContext muleContext;
    private NamespaceManager namespaceManager;

    public JXPathFilter()
    {
        super();
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void initialise() throws InitialisationException
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
            if (namespaces == null)
            {
                namespaces = new HashMap<String, String>(namespaceManager.getNamespaces());
            }
            else
            {
                namespaces.putAll(namespaceManager.getNamespaces());
            }
        }
    }

    public JXPathFilter(String pattern)
    {
        this.pattern = pattern;
    }

    public JXPathFilter(String pattern, String expectedValue)
    {
        this.pattern = pattern;
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean accept(MuleMessage obj)
    {
        if (obj.getPayload() instanceof byte[])
        {
            try
            {
                return accept(obj.getPayloadAsString());
            }
            catch (Exception e)
            {
                logger.warn("JxPath filter rejected message because it could not convert from byte[] to String: " + e.getMessage(), e);
                return false;
            }
        }
        return accept(obj.getPayload());
    }

    private boolean accept(Object obj)
    {
        if (obj == null)
        {
            logger.warn("Applying JXPathFilter to null object.");
            return false;
        }
        if (pattern == null)
        {
            logger.warn("Expression for JXPathFilter is not set.");
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
                    logger.info("Expected value for JXPathFilter is not set, using 'true' by default");
                }
                expectedValue = Boolean.TRUE.toString();
            }
        }

        Object xpathResult = null;
        boolean accept = false;

        Document dom4jDoc;
        try
        {
            dom4jDoc = XMLUtils.toDocument(obj, muleContext);
        }
        catch (Exception e)
        {
            logger.warn("JxPath filter rejected message because of an error while parsing XML: " + e.getMessage(), e);
            return false;
        }

        // Payload is XML
        if (dom4jDoc != null)
        {
            if (namespaces == null)
            {
                // no namespace defined, let's perform a direct evaluation
                xpathResult = dom4jDoc.valueOf(pattern);
            }
            else
            {
                // create an xpath expression with namespaces and evaluate it
                XPath xpath = DocumentHelper.createXPath(pattern);
                xpath.setNamespaceURIs(namespaces);
                xpathResult = xpath.valueOf(dom4jDoc);
            }
        }
        // Payload is a Java object
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Passing object of type " + obj.getClass().getName() + " to JXPathContext");
            }
            JXPathContext context = JXPathContext.newContext(obj);
            initialise(context);
            xpathResult = context.getValue(pattern);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("JXPathFilter Expression result = '" + xpathResult + "' -  Expected value = '"
                    + expectedValue + "'");
        }
        // Compare the XPath result with the expected result.
        if (xpathResult != null)
        {
            accept = xpathResult.toString().equals(expectedValue);
        }
        else
        {
            // A null result was actually expected.
            if (expectedValue.equals("null"))
            {
                accept = true;
            }
            // A null result was not expected, something probably went wrong.
            else
            {
                logger.warn("JXPathFilter expression evaluates to null: " + pattern);
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("JXPathFilter accept object  : " + accept);
        }

        return accept;
    }

    /**
     * Initializes the JXPathContext based on any relevant properties set for the
     * filter.
     *
     * @param context the JXPathContext to initialize
     */
    protected void initialise(JXPathContext context)
    {
        if (namespaces != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Initializing JXPathContext with namespaces: " + namespaces);
            }

            for (Map.Entry<String, String> entry : namespaces.entrySet())
            {
                context.registerNamespace(entry.getKey(), entry.getValue());
            }
        }

        Map.Entry entry;
        if (contextProperties != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Initializing JXPathContext with properties: " + contextProperties);
            }

            for (Iterator iterator = contextProperties.entrySet().iterator(); iterator.hasNext();)
            {
                entry = (Map.Entry) iterator.next();
                context.setValue(entry.getKey().toString(), entry.getValue());
            }
        }

        if (factory != null)
        {
            context.setFactory(factory);
        }

        context.setLenient(lenient);
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
     */
    public void setExpectedValue(String expectedValue)
    {
        this.expectedValue = expectedValue;
    }

    /**
     * @return The expected result value of the XPath expression
     * @deprecated Use <code>getExpectedValue()</code>.
     */
    @Deprecated
    public String getValue()
    {
        return getExpectedValue();
    }

    /**
     * Sets the expected result value of the XPath expression
     *
     * @deprecated Use <code>setExpectedValue(String expectedValue)</code>.
     */
    @Deprecated
    public void setValue(String value)
    {
        setExpectedValue(value);
    }

    public Map<String, String> getNamespaces()
    {
        return namespaces;
    }

    public void setNamespaces(Map<String, String> namespaces)
    {
        this.namespaces = namespaces;
    }

    public Map getContextProperties()
    {
        return contextProperties;
    }

    public void setContextProperties(Map contextProperties)
    {
        this.contextProperties = contextProperties;
    }

    public AbstractFactory getFactory()
    {
        return factory;
    }

    public void setFactory(AbstractFactory factory)
    {
        this.factory = factory;
    }

    public boolean isLenient()
    {
        return lenient;
    }

    public void setLenient(boolean lenient)
    {
        this.lenient = lenient;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final JXPathFilter other = (JXPathFilter) obj;
        return equal(expectedValue, other.expectedValue)
            && equal(contextProperties, other.contextProperties)
            && equal(namespaces, other.namespaces)
            && equal(pattern, other.pattern)
            && lenient == other.lenient;
    }

    @Override
    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), expectedValue, contextProperties, namespaces, pattern, lenient});
    }
}
