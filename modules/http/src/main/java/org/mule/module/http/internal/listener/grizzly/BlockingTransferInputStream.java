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
 * has Transfer-Encoding: chunked or the content is not fully provided because the
 * message is too large.
 *
 * This {@link java.io.InputStream} implementation does a blocking read
 * over the HTTP connection to read the next chunk when there is no more
 * data available.
 */
public class BlockingTransferInputStream extends InputStream
{

    private final FilterChainContext filterChainContext;
    private InputStream chunk;
    private boolean lastPacketReceived;

    public BlockingTransferInputStream(FilterChainContext filterChainContext, InputStream firstChunk)
    {
        this.filterChainContext = filterChainContext;
        this.chunk = firstChunk;
    }

    @Override
    public int read() throws IOException
    {
        int value = chunk.read();
        if (value == -1 && !lastPacketReceived)
        {
            ReadResult readResult = filterChainContext.read();
            HttpContent httpContent = (HttpContent) readResult.getMessage();
            lastPacketReceived = httpContent.isLast();
            chunk = new BufferInputStream(httpContent.getContent());
            value = chunk.read();
        }
        return value;
    }
}
