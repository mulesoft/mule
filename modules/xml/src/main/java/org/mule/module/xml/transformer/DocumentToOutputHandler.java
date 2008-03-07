/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer;

import org.mule.api.MuleEvent;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;

import java.io.IOException;
import java.io.OutputStream;

import org.dom4j.Document;

public class DocumentToOutputHandler extends AbstractXmlTransformer implements DiscoverableTransformer
{

    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

    public DocumentToOutputHandler()
    {
        registerSourceType(Document.class);
        registerSourceType(org.w3c.dom.Document.class);
        setReturnClass(OutputHandler.class);
    }

    public Object doTransform(final Object src, final String encoding) throws TransformerException
    {
        return new OutputHandler()
        {
            public void write(MuleEvent event, OutputStream out) throws IOException
            {
                try
                {
                    out.write(convertToBytes(src, encoding).getBytes());
                }
                catch (javax.xml.transform.TransformerException e)
                {
                    throw new IOException(e.toString());
                }
            }
        };
    }

    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    public void setPriorityWeighting(int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }
    
}
