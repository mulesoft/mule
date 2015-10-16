/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.FlowCallStack;
import org.mule.api.context.notification.ProcessorsTrace;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.security.Credentials;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.ReplyToHandler;
import org.mule.management.stats.ProcessingTime;

import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Set;

/**
 * A {@link VoidMuleEvent} represents a void return from a {@link MessageProcessor} such as a ONE_WAY
 * {@link OutboundEndpoint}.
 */
public class VoidMuleEvent implements MuleEvent
{
    private static final long serialVersionUID = 1418044092304465540L;

    private static final VoidMuleEvent instance = new VoidMuleEvent();

    public static VoidMuleEvent getInstance()
    {
        return instance;
    }

    protected VoidMuleEvent()
    {
        super();
    }

    @Override
    public MuleMessage getMessage()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Credentials getCredentials()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getMessageAsBytes() throws MuleException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object transformMessage() throws TransformerException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T transformMessage(Class<T> outputType) throws TransformerException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T transformMessage(DataType<T> outputType) throws TransformerException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] transformMessageToBytes() throws TransformerException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String transformMessageToString() throws TransformerException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMessageAsString() throws MuleException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMessageAsString(String encoding) throws MuleException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(String name, Object defaultValue)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public MuleSession getSession()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlowConstruct getFlowConstruct()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStopFurtherProcessing()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStopFurtherProcessing(boolean stopFurtherProcessing)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTimeout()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTimeout(int timeout)
    {
        throw new UnsupportedOperationException();

    }

    @Override
    public OutputStream getOutputStream()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getEncoding()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public MuleContext getMuleContext()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProcessingTime getProcessingTime()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public MessageExchangePattern getExchangePattern()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTransacted()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI getMessageSourceURI()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMessageSourceName()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReplyToHandler getReplyToHandler()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getReplyToDestination()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void captureReplyToDestination()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSynchronous()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMessage(MuleMessage message)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getFlowVariable(String key)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataType<?> getFlowVariableDataType(String key)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFlowVariable(String key, Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFlowVariable(String key, Object value, DataType dataType)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFlowVariable(String key)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getFlowVariableNames()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearFlowVariables()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getSessionVariable(String key)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataType<?> getSessionVariableDataType(String key)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSessionVariable(String key, Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSessionVariable(String key, Serializable value, DataType dataType)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSessionVariable(String key)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getSessionVariableNames()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearSessionVariables()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNotificationsEnabled()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEnableNotifications(boolean enabled)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAllowNonBlocking()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlowCallStack getFlowCallStack()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProcessorsTrace getProcessorsTrace()
    {
        throw new UnsupportedOperationException();
    }
}
