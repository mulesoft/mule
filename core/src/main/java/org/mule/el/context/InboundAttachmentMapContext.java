/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
