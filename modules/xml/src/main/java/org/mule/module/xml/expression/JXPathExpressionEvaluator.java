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

import org.mule.api.transport.MessageAdapter;
import org.mule.module.xml.util.XMLUtils;
import org.mule.util.expression.ExpressionEvaluator;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;

/**
 * Will extract properties based on Xpath expressions. Will work on Xml/Dom and beans
 */
public class JXPathExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "jxpath";
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public Object evaluate(String name, MessageAdapter message)
    {

        Object result = null;
        Object payload = message;
        if (message instanceof MessageAdapter)
        {
            payload = ((MessageAdapter) message).getPayload();
        }

        Document dom4jDoc;
        try
        {
            dom4jDoc = XMLUtils.toDocument(payload);
        }
        catch (Exception e)
        {
            logger.error(e);
            return null;
        }
        
        // Payload is XML
        if (dom4jDoc != null)
        {
            result = dom4jDoc.valueOf(name);
        }
        // Payload is a Java object
        else
        {
            JXPathContext context = JXPathContext.newContext(payload);
            try
            {
                result = context.getValue(name);
            }
            catch (Exception e)
            {
                // ignore
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    public String getName()
    {
        return NAME;
    }

    /** {@inheritDoc} */
    public void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }
}
