/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.config;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.mule.umo.UMOMessage;

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
            Document doc = null;
            try {
                doc = DocumentHelper.parseText((String) obj);
            } catch (DocumentException e) {
                logger.error(e);
                return null;
            }
            result = doc.valueOf(name);
        } else {
            JXPathContext context = JXPathContext.newContext(obj);
            result = context.getValue(name);
        }
        return result;
    }
}
