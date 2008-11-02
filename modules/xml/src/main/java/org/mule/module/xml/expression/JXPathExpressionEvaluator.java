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

import org.mule.api.MuleContext;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transport.MessageAdapter;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.xml.i18n.XmlMessages;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.module.xml.util.XMLUtils;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;

/**
 * Will extract properties based on Xpath expressions. Will work on Xml/Dom and beans
 *
 * @deprecated Developers should use xpath, bean or groovy instead of this expression evaluator since there are some
 * quirks with JXPath and the performance is not good.
 */
public class JXPathExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "jxpath";
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private MuleContext muleContext;
    private NamespaceManager namespaceManager;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        try
        {
            namespaceManager = (NamespaceManager) muleContext.getRegistry().lookupObject(NamespaceManager.class);
        }
        catch (RegistrationException e)
        {
            throw new ExpressionRuntimeException(CoreMessages.failedToLoad("NamespaceManager"), e);
        }
    }

    public Object evaluate(String name, MessageAdapter message)
    {
        Object result = null;
        Object payload = message.getPayload();
        JXPathContext context = JXPathContext.newContext(message.getPayload());
        if (namespaceManager != null)
        {
            addNamespaces(namespaceManager, context);
        }

        Document doc;
            try
            {
                //no support for namespaces
                doc = XMLUtils.toDocument(payload);

            }
            catch (Exception e)
            {
                logger.error(e);
                return null;
            }
        //payload is XML
        if(doc!=null)
        {
            result = doc.valueOf(name);
        }
        //payload is a bean
        else
        {
            try
            {
                result = context.getValue(name);
            }
            catch (Exception e)
            {
                // ignore
                if(logger.isDebugEnabled())
                {
                    logger.debug("failed to process JXPath expression: " + name, e);
                }
            }
        }
        return result;
    }

    // Payload is a Java object

    protected void addNamespaces(NamespaceManager manager, JXPathContext context)
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
