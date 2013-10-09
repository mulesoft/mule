/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.filters;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.module.xml.util.XMLUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * <code>IsXmlFilter</code> accepts a String or byte[] if its contents are valid
 * (well-formed) XML.
 */
// @ThreadSafe
public class IsXmlFilter implements Filter
{
    private final XMLInputFactory factory = XMLInputFactory.newInstance();

    // TODO: add validation against a DTD, see MULE-1055

    public IsXmlFilter()
    {
        super();
    }

    public boolean accept(MuleMessage obj)
    {
        return accept(obj.getPayload());
    }

    private boolean accept(Object obj)
    {
        XMLStreamReader parser = null;
        try
        {
            parser = XMLUtils.toXMLStreamReader(factory, obj);
            if (parser == null)
            {
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
