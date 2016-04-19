/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.security.Credentials;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.management.stats.ProcessingTime;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.util.UUID;
import org.mule.tck.MuleTestUtils;

import java.io.OutputStream;
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
            return (byte[]) getMuleContext().getTransformationService().transform(message, DataTypeFactory.BYTE_ARRAY).getPayload();
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
            return (String) getMuleContext().getTransformationService().transform(message, DataTypeFactory.STRING).getPayload();
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
            return (String) getMuleContext().getTransformationService().transform(message, DataTypeFactory
                    .createWithEncoding(String.class, encoding)).getPayload();
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
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
    public String transformMessageToString() throws TransformerException
    {
        try
        {
            return new String(transformMessage(DataType.BYTE_ARRAY_DATA_TYPE), getEncoding());
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
            return MuleTestUtils.getTestFlow(message.getMuleContext());
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
