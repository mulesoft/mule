/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.util;

import org.mule.api.util.StreamCloser;
import org.mule.module.xml.stax.StaxSource;

import javax.xml.stream.XMLStreamReader;

public class XMLStreamCloser implements StreamCloser
{

    public boolean canClose(Class streamType)
    {
        return StaxSource.class.isAssignableFrom(streamType)
               || XMLStreamReader.class.isAssignableFrom(streamType);
    }

    public void close(Object stream) throws Exception
    {
        if (stream instanceof XMLStreamReader)
        {
            ((XMLStreamReader) stream).close();
        }
        else if (stream instanceof StaxSource)
        {
            ((StaxSource) stream).getXMLStreamReader().close();
        }
    }

}
