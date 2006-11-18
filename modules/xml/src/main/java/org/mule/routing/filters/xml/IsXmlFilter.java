/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters.xml;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.xml.sax.InputSource;

/**
 * <code>IsXmlFilter</code> accepts a String or byte[] if its contents are valid
 * (well-formed) XML.
 */
public class IsXmlFilter implements UMOFilter
{

    public IsXmlFilter()
    {
        super();
    }

    public boolean accept(UMOMessage obj)
    {
        return accept(obj.getPayload());
    }

    private boolean accept(Object obj)
    {
        try
        {
            if (obj instanceof String)
            {
                new SAXReader().read(new StringReader((String)obj));
            }
            else if (obj instanceof byte[])
            {
                new SAXReader().read(new InputSource(new ByteArrayInputStream((byte[])obj)));
            }
            else
            {
                throw new DocumentException("Object must be a String or byte array");
            }

            return true;
        }
        catch (DocumentException e)
        {
            return false;
        }
    }

}
