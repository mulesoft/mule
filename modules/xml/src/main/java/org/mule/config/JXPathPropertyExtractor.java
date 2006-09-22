/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Will extract properties based on Xpath expressions.  Will work on
 * Xml/Dom and beans
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JXPathPropertyExtractor extends SimplePropertyExtractor {

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public Object getProperty(String name, UMOMessage message) {

        Object result = null;
        Object obj = message.getPayload();

        if (obj instanceof String) {
            Document doc;
            try {
                doc = DocumentHelper.parseText((String) obj);
            } catch (DocumentException e) {
                logger.error(e);
                return null;
            }
            result = doc.valueOf(name);
        } else {
            JXPathContext context = JXPathContext.newContext(obj);
            try {
                result = context.getValue(name);
            } catch (Exception e) {
                // ignore
            }
        }

        if(result==null) {
            result = super.getProperty(name, message);
        }

        return result;
    }

    public Map getProperties(List names, UMOMessage message) {
        Object result = null;
        Document doc = null;
        JXPathContext context = null;

        Object obj = message.getPayload();
        if (obj instanceof String) {
            try {
                doc = DocumentHelper.parseText((String) obj);
            } catch (DocumentException e) {
                logger.error(e);
                return null;
            }
        } else {
            context = JXPathContext.newContext(obj);
        }

        Map props = new HashMap();

        for (Iterator iterator = names.iterator(); iterator.hasNext();) {
            String name = (String)iterator.next();
            if (doc != null) {
                result = doc.valueOf(name);
            }
            else if (context != null) {
                try {
                    result = context.getValue(name);
                }
                catch (Exception e) {
                    result = null;
                }
            }
            if (result == null) {
                result = super.getProperty(name, message);
            }
            props.put(name, result);
        }

        return props;
    }
}
