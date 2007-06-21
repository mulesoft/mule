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

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * <code>IsXmlFilter</code> accepts a String or byte[] if its contents are valid
 * (well-formed) XML.
 */
// @ThreadSafe
public class IsXmlFilter implements UMOFilter
{
    private final XMLInputFactory factory = XMLInputFactory.newInstance();

    // TODO: add validation against a DTD, see MULE-1055

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
        XMLStreamReader parser = null;

        try
        {
            if (obj instanceof String)
            {
                parser = factory.createXMLStreamReader(new StringReader((String)obj));
            }
            else if (obj instanceof byte[])
            {
                parser = factory.createXMLStreamReader(new ByteArrayInputStream((byte[])obj));
            }
            else
            {
                // neither String nor byte[]
                return false;
            }

            while (parser.next() != XMLStreamConstants.END_DOCUMENT)
            {
                // meep meep!
            }

            return true;
        }
        catch (XMLStreamException ex)
        {
            return false;
        }
        finally
        {
            if (parser != null)
            {
                try
                {
                    parser.close();
                }
                catch (XMLStreamException ignored)
                {
                    // bummer
                }
            }
        }
    }

}
