/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.context;

import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;

import java.util.Set;

import javax.activation.DataHandler;

public class InboundAttachmentMapContext extends AbstractMapContext<String, DataHandler>
{
    private MuleMessage message;

    public InboundAttachmentMapContext(MuleMessage message)
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
        return message.getInboundAttachment((String) key);
    }

    @Override
    public DataHandler put(String key, DataHandler value)
    {
        throw new UnsupportedOperationException(CoreMessages.inboundMessageAttachmentsImmutable(key)
            .getMessage());
    }

    @Override
    public DataHandler remove(Object key)
    {
        throw new UnsupportedOperationException(CoreMessages.inboundMessageAttachmentsImmutable(key)
            .getMessage());
    }

    @Override
    public Set<String> keySet()
    {
        return message.getInboundAttachmentNames();
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException(CoreMessages.inboundMessageAttachmentsImmutable()
            .getMessage());
    }
}
