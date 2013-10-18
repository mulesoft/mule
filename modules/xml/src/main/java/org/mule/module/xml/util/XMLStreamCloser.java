/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
