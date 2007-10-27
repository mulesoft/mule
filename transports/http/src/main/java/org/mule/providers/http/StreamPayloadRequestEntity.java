/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.umo.UMOEvent;
import org.mule.umo.provider.OutputHandler;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.httpclient.methods.RequestEntity;

public class StreamPayloadRequestEntity implements RequestEntity
{
    private OutputHandler outputHandler;
    private UMOEvent event;

    public StreamPayloadRequestEntity(OutputHandler outputHandler, UMOEvent event)
    {
        this.outputHandler = outputHandler;
        this.event = event;
    }

    public boolean isRepeatable()
    {
        return true;
    }

    public void writeRequest(OutputStream outputStream) throws IOException
    {
        outputHandler.write(event, outputStream);
    }

    public long getContentLength()
    {
        return -1L;
    }

    public String getContentType()
    {
        return event.getMessage().getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, 
            HttpConstants.DEFAULT_CONTENT_TYPE);
    }
}

