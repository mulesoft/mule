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

import org.mule.xml.i18n.XmlMessages;

import org.dom4j.Node;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.jaxen.dom4j.Dom4jXPath;
import org.w3c.dom.Document;

/**
 * Will select the text of a single node based on the property name
 */
public class XPathPayloadPropertyExtractor extends AbstractXPathPropertyExtractor
{
    public static final String NAME = "xpath";

    protected XPath createXPath(String expression, Object object) throws JaxenException
    {
        if(object instanceof Document)
        {
            return new DOMXPath(expression);
        }
        else if (object instanceof org.dom4j.Document)
        {
            return new Dom4jXPath(expression);
        }
//        else if (object instanceof nu.xom.Document)
//        {
//            return new XOMXPath(expression);
//        }
//        else if (object instanceof org.jdom.Document)
//        {
//            return new JDOMXPath(expression);
//        }
        else
        {
            throw new IllegalArgumentException(XmlMessages.domTypeNotSupported(object.getClass()).getMessage());
        }
    }

    protected Object extractResultFromNode(Object result)
    {
        if(result instanceof Node)
        {
            return ((Node)result).getText();
        }
        else if(result instanceof org.w3c.dom.Node)
        {
            return ((org.w3c.dom.Node)result).getFirstChild().getNodeValue();
        }
//        else if(result instanceof nu.xom.Node)
//        {
//            return ((nu.xom.Node)result).getText();
//        }
//        else if(result instanceof org.jdom.Node)
//        {
//            return ((org.jdom.Node)result).getText();
//        }
        else
        {
            throw new IllegalArgumentException(XmlMessages.domTypeNotSupported(result.getClass()).getMessage());
        }
    }

    /** {@inheritDoc} */
    public String getName()
    {
        return NAME;
    }
}
