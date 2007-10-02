/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.impl.model.streaming.DelegatingInputStream;

import java.io.InputStream;

/**
 * The {@link TcpMessageDispatcher} and the {@link TcpMessageReceiver} use this
 * class as the input parameter to the read() method on the {@link TcpProtocol}
 * interface. If you wish to simply use the InputStream as the message payload
 * that you're reading in, you just call tcpInputStream.setStreaming(true) so
 * that Mule knows to stop listening for more messages on that stream. 
 */
public class TcpInputStream extends DelegatingInputStream
{
    private boolean streaming;
    
    public TcpInputStream(InputStream delegate)
    {
        super(delegate);
    }

    public boolean isStreaming()
    {
        return streaming;
    }

    public void setStreaming(boolean streaming)
    {
        this.streaming = streaming;
    }

}


