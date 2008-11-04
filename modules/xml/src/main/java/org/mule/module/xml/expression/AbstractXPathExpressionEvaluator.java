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
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.xml.i18n.XmlMessages;
import org.mule.module.xml.util.NamespaceManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.dom4j.Document;
import org.jaxen.JaxenException;
import org.jaxen.XPath;

/**
 * Provides a base class for XPath property extractors. The XPath engine used is jaxen (http://jaxen.org) which supports
 * XPath queries on other object models such as JavaBeans as well as Xml
 */
public abstract class AbstractXPathExpressionEvaluator implements ExpressionEvaluator, Disposable, MuleContextAware
{
    private Map cache = new WeakHashMap(8);

    private MuleContext muleContext;
    private NamespaceManager namespaceManager;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        try
        {
            namespaceManager = (NamespaceManager)muleContext.getRegistry().lookupObject(NamespaceManager.class);
        }
        catch (RegistrationException e)
        {
            throw new ExpressionRuntimeException(CoreMessages.failedToLoad("NamespaceManager"), e);
        }
    }

    /** {@inheritDoc} */
    public Object evaluate(String expression, MuleMessage message)
    {
        try
        {
            Object payload = message.getPayload();
            //we need to convert to a Dom if its an XML string
            if(payload instanceof String)
            {
                payload = message.getPayload(Document.class);
            }

            XPath xpath = getXPath(expression, payload);
            if(namespaceManager!=null)
            {
                addNamespaces(namespaceManager, xpath);
            }

            List result = xpath.selectNodes(payload);
            result = extractResultsFromNodes(result);
            if(result.size()==1)
            {
                return result.get(0);
            }
            else if(result.size()==0)
            {
                return null;
            }
            else
            {
                return result;
            }
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(XmlMessages.failedToProcessXPath(expression), e);
        }
    }

    protected void addNamespaces(NamespaceManager manager, XPath xpath)
    {
        for (Iterator iterator = manager.getNamespaces().entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry)iterator.next();
            try
            {
                xpath.addNamespace(entry.getKey().toString(), entry.getValue().toString());
            }
            catch (JaxenException e)
            {
                throw new ExpressionRuntimeException(XmlMessages.failedToRegisterNamespace(entry.getKey().toString(), entry.getValue().toString()));
            }
        }
    }

    /** {@inheritDoc} */
    public final void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }

    protected XPath getXPath(String expression, Object object) throws JaxenException
    {
        XPath xpath = (XPath)cache.get(expression + getClass().getName());
        if(xpath==null)
        {
            xpath = createXPath(expression, object);
            cache.put(expression + getClass().getName(), xpath);
        }
        return xpath;
    }

    protected abstract XPath createXPath(String expression, Object object) throws JaxenException;

    protected List extractResultsFromNodes(List results)
    {
        if(results==null)
        {
            return null;
        }
        List newResults = new ArrayList(results.size());
        for (Iterator iterator = results.iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            newResults.add(extractResultFromNode(o));
        }
        return newResults;
    }

    /**
     * A lifecycle method where implementor should free up any resources. If an
     * exception is thrown it should just be logged and processing should continue.
     * This method should not throw Runtime exceptions.
     */
    public void dispose()
    {
        cache.clear();
    }

    public NamespaceManager getNamespaceManager()
    {
        return namespaceManager;
    }

    public void setNamespaceManager(NamespaceManager namespaceManager)
    {
        this.namespaceManager = namespaceManager;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    protected abstract Object extractResultFromNode(Object result);
}
