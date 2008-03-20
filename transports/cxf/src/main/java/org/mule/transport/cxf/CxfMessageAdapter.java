/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.transport.AbstractMessageAdapter;
import org.mule.transport.cxf.i18n.CxfMessages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.activation.DataHandler;

import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.AbstractWrappedMessage;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;

/**
 *
 */
public class CxfMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1L;

    private final Message payload;
    
    public CxfMessageAdapter(Message message) throws MuleException
    {
        if (message == null)
        {
            throw new DefaultMuleException(CxfMessages.unableToConstructAdapterForNullMessage());
        }
        this.payload = message;
    }

    /**
     * @return the current payload
     */
    public Object getPayload()
    {
        List<Object> objs = CastUtils.cast(payload.getContent(List.class));

        if (objs == null)
        {
            // Seems Providers get objects stored this way
            Object o = payload.getContent(Object.class);
            if (o != null)
            {
                return o;
            }
            else
            {
                return new Object[0];
            }
        }

        return objs.toArray();
    }

    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        Collection<Attachment> attachments = getAttachments();
        AttachmentImpl newA = new AttachmentImpl(name);
        newA.setDataHandler(dataHandler);
        attachments.add(newA);
    }

    public void removeAttachment(String name) throws Exception
    {
        Collection<Attachment> attachments = getAttachments();
        List<Attachment> newAttachments = new ArrayList<Attachment>();
        for (Attachment attachment : attachments)
        {
            // @TODO: Get some clarify around expected contract, e.g., is <null> an
            // ID? Ever?
            if (attachment.getId() != null && attachment.getId().equals(name))
            {
                continue;
            }
            newAttachments.add(attachment);
        }
        payload.setAttachments(newAttachments);
    }

    protected Collection<Attachment> getAttachments() throws MuleException
    {
        if (payload instanceof AbstractWrappedMessage)
        {
            AbstractWrappedMessage soap = (AbstractWrappedMessage) payload;
            return soap.getAttachments();
        }
        else
        {
            // @TODO: Maybe pass the connector down and use connector exception
            // instead?
            throw new DefaultMuleException(CxfMessages.inappropriateMessageTypeForAttachments(payload.getClass()
                .getName()));
        }
    }
}
