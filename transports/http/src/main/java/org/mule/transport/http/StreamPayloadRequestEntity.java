/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.api.MuleEvent;
import org.mule.api.transport.OutputHandler;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.httpclient.methods.RequestEntity;

public class StreamPayloadRequestEntity implements RequestEntity
{
    private OutputHandler outputHandler;
    private MuleEvent event;

    public StreamPayloadRequestEntity(OutputHandler outputHandler, MuleEvent event)
    {
        this.outputHandler = outputHandler;
        this.event = event;
    }

    public boolean isRepeatable()
    {
        return false;
    }

    public void writeRequest(OutputStream outputStream) throws IOException
    {
        outputHandler.write(event, outputStream);
        outputStream.flush();
    }

    public long getContentLength()
    {
        return -1L;
    }

    public String getContentType()
    {
        return event.getMessage().getOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.DEFAULT_CONTENT_TYPE);
    }
}

