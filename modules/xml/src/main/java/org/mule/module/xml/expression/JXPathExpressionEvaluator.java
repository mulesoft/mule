/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.expression;

import org.apache.commons.jxpath.Container;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.xml.i18n.XmlMessages;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.module.xml.util.XMLUtils;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

/**
 * Will extract properties based on Xpath expressions. Will work on Xml/Dom and beans
 *
 * @deprecated Developers should use xpath, bean or groovy instead of this expression evaluator since there are some
 * quirks with JXPath and the performance is not good.
 */
public class JXPathExpressionEvaluator implements ExpressionEvaluator, MuleContextAware
{
    public static final String NAME = "jxpath";
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private NamespaceManager namespaceManager;

    public void setMuleContext(MuleContext context)
    {
        try
        {
            namespaceManager = context.getRegistry().lookupObject(NamespaceManager.class);
        }
        catch (RegistrationException e)
        {
            throw new ExpressionRuntimeException(CoreMessages.failedToLoad("NamespaceManager"), e);
        }
    }

    public Object evaluate(String expression, MuleMessage message)
    {
        Document document;
        try
        {
            document = XMLUtils.toW3cDocument(message.getPayload());
        }
        catch (Exception e)
        {
            logger.error(e);
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

            public Object getValue()
            {
                return document;
            }

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
        if (namespaceManager != null)
        {
            addNamespacesToContext(namespaceManager, context);
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
        for (Iterator iterator = manager.getNamespaces().entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            try
            {
                context.registerNamespace(entry.getKey().toString(), entry.getValue().toString());
            }
            catch (Exception e)
            {
                throw new ExpressionRuntimeException(XmlMessages.failedToRegisterNamespace(entry.getKey().toString(), entry.getValue().toString()));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    public void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }
}
