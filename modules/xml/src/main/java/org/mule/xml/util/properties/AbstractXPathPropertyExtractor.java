/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.xml.util.properties;

import org.mule.MuleRuntimeException;
import org.mule.umo.UMOMessage;
import org.mule.util.properties.PropertyExtractor;
import org.mule.xml.i18n.XmlMessages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jaxen.JaxenException;
import org.jaxen.XPath;

/**
 * Provides a base class for XPath property extractors. The XPath engine used is jaxen (http://jaxen.org) which supports
 * XPath queries on other object models such as JavaBeans as well as Xml
 */
public abstract class AbstractXPathPropertyExtractor implements PropertyExtractor
{
    private Map cache = new WeakHashMap(8);

    /** {@inheritDoc} */
    public Object getProperty(String expression, Object message)
    {
        try
        {
            if(message instanceof UMOMessage)
            {
                message = ((UMOMessage)message).getPayload();
            }
            XPath xpath = getXPath(expression, message);

            List result = xpath.selectNodes(message);
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
        catch (JaxenException e)
        {
            throw new MuleRuntimeException(XmlMessages.failedToProcessXPath(expression), e);
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
    
    protected abstract Object extractResultFromNode(Object result);
}
