/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.expression;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.expression.AbstractExpressionEvaluator;
import org.mule.module.xml.i18n.XmlMessages;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.module.xml.util.XMLUtils;
import org.mule.util.OneTimeWarning;

import java.util.Map;

import org.apache.commons.jxpath.Container;
import org.apache.commons.jxpath.JXPathContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Will extract properties based on Xpath expressions. Will work on Xml/Dom and beans
 *
 * @deprecated Developers should use xpath, bean or groovy instead of this expression evaluator since there are some
 * quirks with JXPath and the performance is not good.
 */
@Deprecated
public class JXPathExpressionEvaluator extends AbstractExpressionEvaluator implements MuleContextAware
{
    public static final String NAME = "jxpath";
    /**
     * logger used by this class
     */
    protected transient Logger logger = LoggerFactory.getLogger(getClass());
    protected transient MuleContext muleContext;
    private OneTimeWarning deprecationWarning = new OneTimeWarning(logger, "xpath2: expression evaluator has been deprecated in Mule 3.6.0 and will be removed " +
                                                                           "in 4.0. Please use the new xpath3() function instead");
    private NamespaceManager namespaceManager;

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    @Override
    public Object evaluate(String expression, MuleMessage message)
    {
        deprecationWarning.warn();

        Document document;
        try
        {
            document = XMLUtils.toW3cDocument(message.getPayload());
        }
        catch (Exception e)
        {
            logger.error("Could not parse document", e);
            return null;
        }

        JXPathContext context;

        if (document != null)
        {
            context = createContextForXml(document);
        }
        else
        {
            context = createContextForBean(message.getPayload());
        }

        return getExpressionValue(context, expression);
    }

    private JXPathContext createContextForXml(final Document document)
    {
        Container container = new Container()
        {

            @Override
            public Object getValue()
            {
                return document;
            }

            @Override
            public void setValue(Object value)
            {
                throw new UnsupportedOperationException();
            }
        };

        return JXPathContext.newContext(container);
    }

    private JXPathContext createContextForBean(Object payload)
    {
        return JXPathContext.newContext(payload);
    }

    private Object getExpressionValue(JXPathContext context, String expression)
    {
        NamespaceManager theNamespaceManager = getNamespaceManager();
        if (theNamespaceManager != null)
        {
            addNamespacesToContext(theNamespaceManager, context);
        }

        Object result = null;

        try
        {
            result = context.getValue(expression);
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("failed to process JXPath expression: " + expression, e);
            }
        }

        return result;
    }

    protected void addNamespacesToContext(NamespaceManager manager, JXPathContext context)
    {
        for (Map.Entry<String, String> entry : manager.getNamespaces().entrySet())
        {
            try
            {
                context.registerNamespace(entry.getKey(), entry.getValue());
            }
            catch (Exception e)
            {
                throw new ExpressionRuntimeException(XmlMessages.failedToRegisterNamespace(entry.getKey(), entry.getValue()));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return NAME;
    }

    /**
     *
     * @return the nsmespace manager from the registry
     */
    protected synchronized NamespaceManager getNamespaceManager()
    {
        if (namespaceManager == null)
        {

            try
            {
                // We defer looking this up until registry is completely built
                if (muleContext != null)
                {
                    namespaceManager = muleContext.getRegistry().lookupObject(NamespaceManager.class);
                }
            }
            catch (RegistrationException e)
            {
                throw new ExpressionRuntimeException(CoreMessages.failedToLoad("NamespaceManager"), e);
            }
        }
        return namespaceManager;
    }
}
