/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

