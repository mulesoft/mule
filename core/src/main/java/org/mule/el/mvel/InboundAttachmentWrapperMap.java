/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;
import org.mule.el.AbstractExpressionLanguageMap;

import java.util.Set;

import javax.activation.DataHandler;

import org.mvel2.ImmutableElementException;

class InboundAttachmentWrapperMap extends AbstractExpressionLanguageMap<String, DataHandler>
{
    private MuleMessage message;

    public InboundAttachmentWrapperMap(MuleMessage message)
    {
        this.message = message;
    }

    @Override
    public void clear()
    {
        throw new ImmutableElementException(CoreMessages.inboundMessageAttachmentsImmutable().getMessage());
    }

    @Override
    public boolean containsKey(Object key)
    {
        return message.getInboundAttachmentNames().contains(key);
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
    public Set<String> keySet()
    {
        return message.getInboundPropertyNames();
    }

    @Override
    public DataHandler put(String key, DataHandler value)
    {
        throw new ImmutableElementException(CoreMessages.inboundMessageAttachmentsImmutable(key).getMessage());
    }

    @Override
    public DataHandler remove(Object key)
    {
        throw new ImmutableElementException(CoreMessages.inboundMessageAttachmentsImmutable(key).getMessage());
    }
}
