/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.dom4j.io.DOMReader;
import org.w3c.dom.Document;

/**
 * General utility methods for working with XML.
 */
public class XMLUtils
{
    /**
     * Converts a DOM to an XML string.
     */
    public static String toXml(Document dom)
    {
        return new DOMReader().read(dom).asXML();
    }
}


