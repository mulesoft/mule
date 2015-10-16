/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.MessageExchangePattern;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.FlowCallStack;
import org.mule.api.context.notification.ProcessorsTrace;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.security.Credentials;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.ReplyToHandler;
import org.mule.management.stats.ProcessingTime;
import org.mule.tck.MuleTestUtils;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.UUID;

import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * An event used for outbound routing tests. It is not fully fleshed out, containing only the information
 * needed for routing.
 */
public class OutboundRoutingTestEvent implements MuleEvent
{
    private MuleMessage message;
    private MuleSession session;
    private String id = UUID.getUUID();
    private boolean stopFurtherProcessing;
    int timeout = -1;
    private InboundEndpoint endpoint;

    public OutboundRoutingTestEvent(MuleMessage message, MuleSession session, MuleContext muleContext)
        throws Exception
    {
        this.message = message;
        this.session = session;
        this.endpoint = MuleTestUtils.getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE,
            muleContext);
    }

    @Override
    public MuleMessage getMessage()
    {
        return message;
    }

    @Override
    public MuleSession getSession()
    {
        return session;
    }

    @Override
    public Credentials getCredentials()
    {
        return null;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Object transformMessage() throws TransformerException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T transformMessage(Class<T> outputType) throws TransformerException
    {
        return transformMessage(DataTypeFactory.create(outputType));
    }

    @Override
    public <T> T transformMessage(DataType<T> outputType) throws TransformerException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public byte[] transformMessageToBytes() throws TransformerException
    {
        return transformMessage(DataType.BYTE_ARRAY_DATA_TYPE);
    }

    @Override
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

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public Object getProperty(String name)
    {
        return null;
    }

    @Override
    public Object getProperty(String name, Object defaultValue)
    {
        return defaultValue;
    }

    @Override
    public boolean isStopFurtherProcessing()
    {
        return stopFurtherProcessing;
    }

    @Override
    public void setStopFurtherProcessing(boolean stopFurtherProcessing)
    {
        this.stopFurtherProcessing = stopFurtherProcessing;
    }

    @Override
    public int getTimeout()
    {
        return timeout;
    }

    @Override
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    @Override
    public OutputStream getOutputStream()
    {
        return null;
    }

    @Override
    public String getEncoding()
    {
        return message.getEncoding();
    }

    @Override
    public MuleContext getMuleContext()
    {
        return null;
    }

    @Override
    public FlowConstruct getFlowConstruct()
    {
        try
        {
            return MuleTestUtils.getTestService(message.getMuleContext());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProcessingTime getProcessingTime()
    {
        return null;
    }

    @Override
    public MessageExchangePattern getExchangePattern()
    {
        return endpoint.getExchangePattern();
    }

    @Override
    public boolean isTransacted()
    {
        return false;
    }

    @Override
    public URI getMessageSourceURI()
    {
        return URI.create("test://test");
    }

    @Override
    public String getMessageSourceName()
    {
        return "test";
    }

    @Override
    public ReplyToHandler getReplyToHandler()
    {
        return null;
    }

    @Override
    public Object getReplyToDestination()
    {
        return null;
    }

    @Override
    public void captureReplyToDestination()
    {
    }

    @Override
    public boolean isSynchronous()
    {
        return false;
    }

    @Override
    public void setMessage(MuleMessage message)
    {
    }

    @Override
    public DataType<?> getFlowVariableDataType(String key)
    {
        return null;
    }

    @Override
    public Object getFlowVariable(String key)
    {
        return null;
    }

    @Override
    public void setFlowVariable(String key, Object value)
    {
    }

    @Override
    public void setFlowVariable(String key, Object value, DataType dataType)
    {

    }

    @Override
    public void removeFlowVariable(String key)
    {
    }

    @Override
    public Set<String> getFlowVariableNames()
    {
        return new HashSet<String>();
    }

    @Override
    public void clearFlowVariables()
    {
    }

    @Override
    public DataType<?> getSessionVariableDataType(String key)
    {
        return null;
    }

    @Override
    public Object getSessionVariable(String key)
    {
        return null;
    }

    @Override
    public void setSessionVariable(String key, Object value)
    {
    }

    @Override
    public void setSessionVariable(String key, Serializable value, DataType dataType)
    {

    }

    @Override
    public void removeSessionVariable(String key)
    {
    }

    @Override
    public Set<String> getSessionVariableNames()
    {
        return new HashSet<String>();
    }

    @Override
    public void clearSessionVariables()
    {
    }

    @Override
    public boolean isNotificationsEnabled()
    {
        return true;
    }

    @Override
    public void setEnableNotifications(boolean enabled)
    {
    }

    @Override
    public boolean isAllowNonBlocking()
    {
        return false;
    }

    @Override
    public FlowCallStack getFlowCallStack()
    {
        return null;
    }

    @Override
    public ProcessorsTrace getProcessorsTrace()
    {
        return null;
    }
}
