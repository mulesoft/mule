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
import org.mule.api.registry.RegistrationException;
import org.mule.api.routing.filter.Filter;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.module.xml.util.XMLUtils;
import org.mule.transformer.types.DataTypeFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.dom.DOMSource;

import org.apache.commons.jxpath.AbstractFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.jaxen.BaseXPath;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.jaxen.dom4j.Dom4jXPath;
import org.jaxen.javabean.JavaBeanXPath;

/**
 * <code>JaxenFilter</code> evaluates an XPath expression against an XML document
 * using Jaxen.
 *
 * @deprecated This feature is deprecated and will be removed in Mule 4.0. Use xpath-filter instead
 */
@Deprecated
public class JaxenFilter implements Filter, MuleContextAware
{
    protected transient Log logger = LogFactory.getLog(getClass());

    private String pattern;
    private String expectedValue;
    private Map<String, String> namespaces = null;
    private Map contextProperties = null;
    private AbstractFactory factory;

    private MuleContext muleContext;
    private NamespaceManager namespaceManager;

    public JaxenFilter()
    {
        super();
    }

    public JaxenFilter(String pattern)
    {
        this.pattern = pattern;
    }

    public JaxenFilter(String pattern, String expectedValue)
    {
        this.pattern = pattern;
        this.expectedValue = expectedValue;
    }


    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        try
        {
            namespaceManager = muleContext.getRegistry().lookupObject(NamespaceManager.class);
        }
        catch (RegistrationException e)
        {
            throw new ExpressionRuntimeException(CoreMessages.failedToLoad("NamespaceManager"), e);
        }
        if (namespaceManager!=null)
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

    @Override
    public boolean accept(MuleMessage obj)
    {
        Object payload = obj.getPayload();

        try
        {
            // Ensure that we have an object we can run an XPath on
            if (payload instanceof DOMSource)
            {
                accept(((DOMSource) payload).getNode());
            }
            else if (payload instanceof byte[]
                     || payload instanceof InputStream
                     || payload instanceof String)
            {
                try
                {
                    return accept(obj.getPayload(DataTypeFactory.create(org.w3c.dom.Document.class)));
                }
                catch (Exception e)
                {
                    logger.warn("JaxenPath filter rejected message because it could not convert from "
                            + payload.getClass()
                            + " to Source: "+ e.getMessage(), e);
                    return false;
                }
            }

            return accept(payload);
        }
        catch (JaxenException e)
        {
            logger.warn("JaxenPath filter rejected message because it could not build/evaluate the XPath expression.", e);
            return false;
        }
    }

    private boolean accept(Object obj) throws JaxenException
    {
        if (obj == null)
        {
            logger.warn("Applying JaxenFilter to null object.");
            return false;
        }
        if (pattern == null)
        {
            logger.warn("Expression for JaxenFilter is not set.");
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
                    logger.info("Expected value for JaxenFilter is not set, using 'true' by default");
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
            throw new JaxenException(e);
        }

        // Payload is a DOM Document
        if (dom4jDoc != null)
        {
            xpathResult = getDom4jXPath().stringValueOf(dom4jDoc);
        }
        // Payload is a W3C Document
        else if (obj instanceof DOMSource)
        {
            xpathResult = getDOMXPath().stringValueOf(obj);
        }
        // Payload is a W3C Document
        else if (obj instanceof org.w3c.dom.Document)
        {
            xpathResult = getDOMXPath().stringValueOf(obj);
        }
        // Payload is a Java object
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Passing object of type " + obj.getClass().getName() + " to JaxenContext");
            }
            xpathResult = getJavaBeanXPath().stringValueOf(obj);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("JaxenFilter Expression result = '" + xpathResult + "' -  Expected value = '"
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
                logger.warn("JaxenFilter expression evaluates to null: " + pattern);
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("JaxenFilter accept object  : " + accept);
        }

        return accept;
    }

    protected DOMXPath getDOMXPath() throws JaxenException
    {
        DOMXPath xpath = new DOMXPath(pattern);
        setupNamespaces(xpath);
        return xpath;
    }

    protected Dom4jXPath getDom4jXPath() throws JaxenException
    {
        Dom4jXPath xpath = new Dom4jXPath(pattern);
        setupNamespaces(xpath);
        return xpath;
    }

    protected JavaBeanXPath getJavaBeanXPath() throws JaxenException
    {
        JavaBeanXPath xpath = new JavaBeanXPath(pattern);
        setupNamespaces(xpath);
        return xpath;
    }

    private void setupNamespaces(BaseXPath xpath) throws JaxenException
    {
        if (namespaces != null)
        {
            for (Map.Entry<String, String>entry : namespaces.entrySet())
            {
                xpath.addNamespace(entry.getKey(), entry.getValue());
            }
        }
    }

    /** @return XPath expression */
    public String getPattern()
    {
        return pattern;
    }

    /** @param pattern The XPath expression */
    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    /** @return The expected result value of the XPath expression */
    public String getExpectedValue()
    {
        return expectedValue;
    }

    /** Sets the expected result value of the XPath expression */
    public void setExpectedValue(String expectedValue)
    {
        this.expectedValue = expectedValue;
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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final JaxenFilter other = (JaxenFilter) obj;
        return equal(expectedValue, other.expectedValue)
            && equal(contextProperties, other.contextProperties)
            && equal(namespaces, other.namespaces)
            && equal(pattern, other.pattern);
    }

    @Override
    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), expectedValue, contextProperties, namespaces, pattern});
    }
}
