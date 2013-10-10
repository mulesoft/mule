/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.stdio;

import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractPollingMessageReceiver;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

/**
 * <code>StdioConnector</code> can send and receive Mule events over IO streams.
 */

public abstract class StdioConnector extends AbstractConnector
{

    public static final String STDIO = "stdio";
    public static final String STREAM_SYSTEM_IN = "system.in";
    public static final String STREAM_SYSTEM_OUT = "system.out";
    public static final String STREAM_SYSTEM_ERR = "system.err";

    protected OutputStream outputStream;
    protected InputStream inputStream;

    public StdioConnector(MuleContext context)
    {
        super(context);
    }
    
    @Override
    public MessageReceiver createReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint) throws Exception
    {
        return serviceDescriptor.createMessageReceiver(this, flowConstruct, endpoint,
                                                       AbstractPollingMessageReceiver.DEFAULT_POLL_FREQUENCY);
    }

    @Override
    public void doStop()
    {
        // template method
    }

    @Override
    protected void doDispose()
    {
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(outputStream);
    }

    @Override
    public void doStart()
    {
        // template method
    }

    public String getProtocol()
    {
        return STDIO;
    }

    public InputStream getInputStream()
    {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    public OutputStream getOutputStream()
    {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

    public void registerListener(InboundEndpoint endpoint, MessageProcessor listener, FlowConstruct flowConstruct) throws Exception
    {
        if (receivers.size() > 0)
        {
            throw new UnsupportedOperationException(
                "You can only register one listener per system stream connector");
        }
        super.registerListener(endpoint, listener, flowConstruct);
    }
}
