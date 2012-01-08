/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.security.Credentials;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.ReplyToHandler;
import org.mule.management.stats.ProcessingTime;
import org.mule.tck.MuleTestUtils;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.UUID;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/** An event used for outbound routing tests.  It is not fully fleshed out, containing only the information needed for
 * routing.
 */
public class OutboundRoutingTestEvent implements MuleEvent
{
    private MuleMessage message;
    private MuleSession session;
    private String id = UUID.getUUID();
    private boolean stopFurtherProcessing;
    private ImmutableEndpoint endpoint;
    int timeout = -1;

    public OutboundRoutingTestEvent(MuleMessage message, MuleSession session)
    {
        this.message = message;
        this.session = session;
    }

    public OutboundRoutingTestEvent(MuleMessage message, MuleSession session, ImmutableEndpoint endpoint)
    {
        this.message = message;
        this.session = session;
        this.endpoint = endpoint;
    }

    public MuleMessage getMessage()
    {
        return message;
    }

    public MuleSession getSession()
    {
        return session;
    }

    public Credentials getCredentials()
    {
        return null;
    }

    public byte[] getMessageAsBytes() throws MuleException
    {
        try
        {
            return message.getPayloadAsBytes();
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }

    public String getMessageAsString() throws MuleException
    {
        try
        {
            return message.getPayloadAsString();
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }

    public String getMessageAsString(String encoding) throws MuleException
    {
        try
        {
            return message.getPayloadAsString(encoding);
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }

    public Object transformMessage() throws TransformerException
    {
        throw new UnsupportedOperationException();
    }

    public <T> T transformMessage(Class<T> outputType) throws TransformerException
    {
        return (T)transformMessage(DataTypeFactory.create(outputType));
    }

    public <T> T transformMessage(DataType<T> outputType) throws TransformerException
    {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public byte[] transformMessageToBytes() throws TransformerException
    {
        return transformMessage(DataType.BYTE_ARRAY_DATA_TYPE);
    }

    public String transformMessageToString() throws TransformerException
    {
        try
        {
            return new String(transformMessageToBytes(), getEncoding());
        }
        catch (UnsupportedEncodingException e)
        {
            return "Unsupported Encoding";
        }
    }

    public String getId()
    {
        return id;
    }

    public Object getProperty(String name)
    {
        return null;
    }

    public Object getProperty(String name, Object defaultValue)
    {
        return defaultValue;
    }

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    public FlowConstruct getService()
    {
        return null;
    }

    public boolean isStopFurtherProcessing()
    {
        return stopFurtherProcessing;
    }

    public void setStopFurtherProcessing(boolean stopFurtherProcessing)
    {
        this.stopFurtherProcessing = stopFurtherProcessing;
    }

    public boolean isSynchronous()
    {
        return false;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public OutputStream getOutputStream()
    {
        return null;
    }

    public String getEncoding()
    {
        return message.getEncoding();
    }

    public MuleContext getMuleContext()
    {
        return null;
    }

    public FlowConstruct getFlowConstruct()
    {
        try
        {
            return session == null
                                  ? MuleTestUtils.getTestService(message.getMuleContext())
                                  : session.getFlowConstruct();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public ProcessingTime getProcessingTime()
    {
        return null;
    }

    public ReplyToHandler getReplyToHandler() {
        return null;
    }

    public Object getReplyToDestination() {
        return null;
    }

    public void captureReplyToDestination() {}
}
