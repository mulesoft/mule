/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.stdio;

import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractPollingMessageReceiver;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.PushbackInputStream;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.SystemUtils;

/**
 * <code>StdioMessageReceiver</code> is a listener for events from Mule components
 * which then simply passes the events on to the target components.
 */
public class StdioMessageReceiver extends AbstractPollingMessageReceiver
{
    public static final int DEFAULT_BUFFER_SIZE = 4096;

    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private InputStream inputStream;
    private StdioConnector connector;

    private boolean sendStream;

    public StdioMessageReceiver(Connector connector,
                                FlowConstruct flowConstruct,
                                InboundEndpoint endpoint,
                                long checkFrequency) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        this.setFrequency(checkFrequency);

        this.connector = (StdioConnector) connector;
        String streamName = endpoint.getEndpointURI().getAddress();
        if (StdioConnector.STREAM_SYSTEM_IN.equalsIgnoreCase(streamName))
        {
            inputStream = System.in;
        }
        else
        {
            inputStream = this.connector.getInputStream();
        }

        // apply connector-specific properties
        if (connector instanceof PromptStdioConnector)
        {
            PromptStdioConnector ssc = (PromptStdioConnector) connector;

            String promptMessage = (String) endpoint.getProperties().get("promptMessage");
            if (promptMessage != null)
            {
                ssc.setPromptMessage(promptMessage);
            }
        }
        
        this.sendStream = BooleanUtils.toBoolean((String) endpoint.getProperties().get("sendStream"));
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    public void doConnect() throws Exception
    {
        if (connector instanceof PromptStdioConnector)
        {
            PromptStdioConnector ssc = (PromptStdioConnector) connector;
            DelayedMessageWriter writer = new DelayedMessageWriter(ssc);
            writer.start();
        }
    }

    @Override
    public void doDisconnect() throws Exception
    {
        // noop
    }

    @Override
    public void poll()
    {
        String encoding = endpoint.getEncoding();
        try
        {
            if (sendStream)
            {
                PushbackInputStream in = new PushbackInputStream(inputStream);

                //Block until we have some data
                int i = in.read();
                //Roll back our read
                in.unread(i);
                MuleMessage message = createMuleMessage(in, encoding);
                routeMessage(message);
            }
            else
            {
                byte[] inputBuffer = new byte[bufferSize];
                int len = inputStream.read(inputBuffer);

                if (len == -1)
                {
                    return;
                }

                StringBuilder fullBuffer = new StringBuilder(bufferSize);
                while (len > 0)
                {
                    fullBuffer.append(new String(inputBuffer, 0, len));
                    len = 0; // mark as read
                    if (inputStream.available() > 0)
                    {
                        len = inputStream.read(inputBuffer);
                    }
                }

                // Each line is a separate message
                String[] lines = fullBuffer.toString().split(SystemUtils.LINE_SEPARATOR);
                for (int i = 0; i < lines.length; ++i)
                {                
                    MuleMessage message = createMuleMessage(lines[i], encoding);
                    routeMessage(message);
                }
            }

            doConnect();
        }
        catch (Exception e)
        {
            getEndpoint().getMuleContext().getExceptionListener().handleException(e);
        }
    }

    public InputStream getInputStream()
    {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    public int getBufferSize()
    {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize)
    {
        this.bufferSize = bufferSize;
    }

    private class DelayedMessageWriter extends Thread
    {
        private long delay = 0;
        private PromptStdioConnector ssc;

        public DelayedMessageWriter(PromptStdioConnector ssc)
        {
            this.delay = ssc.getMessageDelayTime();
            this.ssc = ssc;
        }

        @Override
        public void run()
        {
            if (delay > 0)
            {
                try
                {
                    // Allow all other console message to be printed out first
                    sleep(delay);
                }
                catch (InterruptedException e1)
                {
                    // ignore
                }
            }
            ((PrintStream) ssc.getOutputStream()).println();
            ((PrintStream) ssc.getOutputStream()).print(ssc.getPromptMessage());
        }
    }
}
