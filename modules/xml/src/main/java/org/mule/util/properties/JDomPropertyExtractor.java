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

import org.mule.umo.UMOMessage;

import org.dom4j.Node;
import org.dom4j.io.DOMReader;

/**
 * Will select the text of a single node based on the property name
 */
public class JDomPropertyExtractor implements PropertyExtractor
{
    public Object getProperty(String name, Object message)
    {
        Object payload = message;
        if (message instanceof UMOMessage)
        {
            payload = ((UMOMessage)message).getPayload();
        }
        if (payload instanceof org.w3c.dom.Document)
        {
            org.w3c.dom.Document x3cDoc = (org.w3c.dom.Document)payload;
            org.dom4j.Document dom4jDoc = new DOMReader().read(x3cDoc);
            try
            {
                Node node = dom4jDoc.selectSingleNode(name);
                if (node != null)
                {
                    return node.getText();
                }
            }
            catch (Exception ignored)
            {
                // ignore
            }
        }
        return null;
    }
}
