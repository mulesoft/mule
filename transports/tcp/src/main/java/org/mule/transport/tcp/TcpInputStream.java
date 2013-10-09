/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.model.streaming.DelegatingInputStream;

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


