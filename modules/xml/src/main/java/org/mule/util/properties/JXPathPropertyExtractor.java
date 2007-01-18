/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.properties;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.mule.umo.UMOMessage;

/**
 * Will extract properties based on Xpath expressions. Will work on Xml/Dom and beans
 */
public class JXPathPropertyExtractor implements PropertyExtractor
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public Object getProperty(String name, Object message)
    {

        Object result = null;
        Object payload = message;
        if (message instanceof UMOMessage)
        {
            payload = ((UMOMessage) message).getPayload();
        }

        if (payload instanceof String)
        {
            Document doc;
            try
            {
                doc = DocumentHelper.parseText((String) payload);
            }
            catch (DocumentException e)
            {
                logger.error(e);
                return null;
            }
            result = doc.valueOf(name);
        }
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
}
