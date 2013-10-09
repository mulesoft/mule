/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.context;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;

import java.util.Set;

import javax.activation.DataHandler;

public class OutboundAttachmentMapContext extends AbstractMapContext<String, DataHandler>
{
    private MuleMessage message;

    public OutboundAttachmentMapContext(MuleMessage message)
    {
        this.message = message;
    }

    @Override
    public DataHandler get(Object key)
    {
        if (!(key instanceof String))
        {
            return null;
        }

        return message.getOutboundAttachment((String) key);
    }

    @Override
    public DataHandler put(String key, DataHandler value)
    {
        DataHandler previousValue = get(key);
        try
        {
            message.addOutboundAttachment(key, (DataHandler) value);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
        return previousValue;
    }

    @Override
    public DataHandler remove(Object key)
    {
        if (!(key instanceof String))
        {
            return null;
        }

        DataHandler previousValue = get(key);
        try
        {
            message.removeOutboundAttachment((String) key);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return previousValue;
    }

    @Override
    public Set<String> keySet()
    {
        return message.getOutboundAttachmentNames();
    }

}
