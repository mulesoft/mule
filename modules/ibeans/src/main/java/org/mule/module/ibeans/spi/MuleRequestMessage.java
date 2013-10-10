/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.spi;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.transport.PropertyScope;

import java.util.Set;

import javax.activation.DataHandler;

import org.ibeans.api.IBeanInvocationData;
import org.ibeans.api.Request;

/**
 * An implementation of an IBeans {@link org.ibeans.api.Request} that adapts to a {@link org.mule.api.MuleMessage}
 */
public class MuleRequestMessage implements Request
{
    private MuleMessage message;
    private int timeout = 0;
    private IBeanInvocationData data;

    public MuleRequestMessage(IBeanInvocationData data, MuleMessage message)
    {
        this.message = message;
        this.data = data;
    }

    public Object getPayload()
    {
        return message.getPayload();
    }

    public void setPayload(Object payload)
    {
        message.setPayload(payload);
    }

    public void addHeader(String name, Object value)
    {
        message.setOutboundProperty(name, value);
    }

    public Object removeHeader(String name)
    {
        return message.removeProperty(name, PropertyScope.OUTBOUND);
    }

    public Object getHeader(String name)
    {
        return message.getOutboundProperty(name);
    }

    public Set<String> getHeaderNames()
    {
        return message.getOutboundPropertyNames();
    }

    public void addAttachment(String name, DataHandler handler)
    {
        try
        {
            message.addOutboundAttachment(name, handler);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    public DataHandler removeAttachment(String name)
    {
        DataHandler dh = message.getOutboundAttachment(name);
        try
        {
            if(dh!=null)
            message.removeOutboundAttachment(name);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
        return dh;
    }

    public DataHandler getAttachment(String name)
    {
        return message.getOutboundAttachment(name);
    }

    public Set<String> getAttachmentNames()
    {
        return message.getOutboundAttachmentNames();
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public IBeanInvocationData getIBeanInvocationData()
    {
        return data;
    }

    public MuleMessage getMessage()
    {
        return message;
    }
}
