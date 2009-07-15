/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email;

import org.mule.api.MuleException;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;

/**
 * <code>MailMessageAdapter</code> is a wrapper for a javax.mail.Message that
 * separates multi-part mail messages, storing all but the first part as attachments
 * to the underlying {@link org.mule.transport.AbstractMessageAdapter}.  Alternatively, you can use
 * {@link org.mule.transport.email.SimpleMailMessageAdapter}, which stores the message as a single
 * entity.
 */
public class EagerMailMessageAdapter extends MailMessageAdapter
{

    private static final long serialVersionUID = -6013198455030918360L;
    public static final String ATTACHMENT_HEADERS_PROPERTY_POSTFIX = "Headers";

    private Object payload;

    public EagerMailMessageAdapter(Object object) throws MuleException
    {
        super(object);
    }

    /**
     * Add mime body parts as attachments.
     */
    @Override
    protected void handleMessage(Message message) throws Exception
    {
        payload = message.getContent();

        if (payload instanceof Multipart)
        {
            TreeMap attachments = new TreeMap();
            MailUtils.getAttachments((Multipart) payload, attachments);

            logger.debug("Received Multipart message. Adding attachments");
            for (Iterator iterator = attachments.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry entry = (Map.Entry) iterator.next();
                Part part = (Part) entry.getValue();
                String name = entry.getKey().toString();

                addAttachment(name, part.getDataHandler());
                addAttachmentHeaders(name, part);
            }
        }
    }

    @Override
    public Object getPayload()
    {
        return payload;
    }
}