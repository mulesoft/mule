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

/**
 * Will select the text of a single node based on the property name
 */
public class Dom4jPropertyExtractor implements PropertyExtractor
{
    public Object getProperty(String name, Object message)
    {
        Object payload = message;
        if (message instanceof UMOMessage)
        {
            payload = ((UMOMessage)message).getPayload();
        }
        if (payload instanceof org.dom4j.Document)
        {
            org.dom4j.Document dom4jDoc = (org.dom4j.Document)payload;
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
        else if (payload instanceof org.dom4j.Node)
        {
            org.dom4j.Node dom4jNode = (org.dom4j.Node)payload;
            try
            {
                Node node = dom4jNode.selectSingleNode(name);
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
