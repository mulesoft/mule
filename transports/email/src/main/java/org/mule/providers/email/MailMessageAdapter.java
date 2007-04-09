/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;

/**
 * <code>MailMessageAdapter</code> is a wrapper for a javax.mail.Message that 
 * separates multi-part mail messages, storing all but the first part as attachments 
 * to the underlying {@link AbstractMessageAdapter}.  Alternatively, you can use
 * {@link SimpleMailMessageAdapter}, which stores the message as a single
 * entity.
 */
public class MailMessageAdapter extends SimpleMailMessageAdapter
{

    private static final long serialVersionUID = -6013198455030918360L;
    public static final String ATTACHMENT_HEADERS_PROPERTY_POSTFIX = "Headers";

    public MailMessageAdapter(Object object) throws MessagingException
    {
        super(object);
    }

    /**
     * Store only the first body part directly; add further parts as attachments.
     */
    // @Override
    protected void handleMessage(Message message) throws Exception
    {
        Object content = message.getContent();

        if (content instanceof Multipart)
        {
            setMessage(((Multipart)content).getBodyPart(0));
            logger.debug("Received Multipart message");

            for (int i = 1; i < ((Multipart)content).getCount(); i++)
            {
                Part part = ((Multipart)content).getBodyPart(i);
                String name = part.getFileName();
                if (name == null)
                {
                    name = String.valueOf(i - 1);
                }
                addAttachment(name, part.getDataHandler());
                addAttachmentHeaders(name, part);
            }
        }
        else
        {
            setMessage(message);
        }
    }

    protected void addAttachmentHeaders(String name, Part part) throws javax.mail.MessagingException
    {
        Map headers = new HashMap(4);
        for (Enumeration e = part.getAllHeaders(); e.hasMoreElements();)
        {
            Header h = (Header)e.nextElement();
            headers.put(h.getName(), h.getValue());
        }
        if (headers.size() > 0)
        {
            setProperty(name + ATTACHMENT_HEADERS_PROPERTY_POSTFIX, headers);
        }
    }

}
