/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import java.io.IOException;
import java.io.InputStream;

import org.glassfish.grizzly.ReadResult;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.utils.BufferInputStream;

/**
 * {@link java.io.InputStream} to be used when the HTTP request
 * has Transfer-Encoding: chunked.
 *
 * This {@link java.io.InputStream} implementation does a blocking read
 * over the HTTP connection to read the next chunk when there is no more
 * data available.
 */
public class TransferEncodingChunkInputStream extends InputStream
{

    private final FilterChainContext filterChainContext;
    private InputStream grizzlyContent;
    private boolean lastPacketReceived;

    public TransferEncodingChunkInputStream(FilterChainContext filterChainContext, InputStream grizzlyContent)
    {
        this.filterChainContext = filterChainContext;
        this.grizzlyContent = grizzlyContent;
    }

    @Override
    public int read() throws IOException
    {
        int value = grizzlyContent.read();
        if (value == -1 && !lastPacketReceived)
        {
            ReadResult readResult = filterChainContext.read();
            HttpContent httpContent = (HttpContent) readResult.getMessage();
            lastPacketReceived = httpContent.isLast();
            grizzlyContent = new BufferInputStream(httpContent.getContent());
            value = grizzlyContent.read();
        }
        return value;
    }
}
