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

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.transport.AbstractMuleMessageFactory;
import org.mule.util.StringUtils;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MailMuleMessageFactory extends AbstractMuleMessageFactory
{
    public static final String HEADER_LIST_PREFIX = "List:";

    private static Log log = LogFactory.getLog(MailMuleMessageFactory.class);
    
    public MailMuleMessageFactory(MuleContext context)
    {
        super(context);
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[]{Message.class};
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        return transportMessage;
    }
    
    @Override
    protected void addProperties(MuleMessage muleMessage, Object transportMessage) throws Exception
    {
        super.addProperties(muleMessage, transportMessage);

        Message mailMessage = (Message) transportMessage;
        
        muleMessage.setProperty(MailProperties.INBOUND_TO_ADDRESSES_PROPERTY,
            MailUtils.mailAddressesToString(mailMessage.getRecipients(Message.RecipientType.TO)));
        muleMessage.setProperty(MailProperties.INBOUND_CC_ADDRESSES_PROPERTY,
            MailUtils.mailAddressesToString(mailMessage.getRecipients(Message.RecipientType.CC)));
        muleMessage.setProperty(MailProperties.INBOUND_BCC_ADDRESSES_PROPERTY,
            MailUtils.mailAddressesToString(mailMessage.getRecipients(Message.RecipientType.BCC)));
        try
        {
            muleMessage.setProperty(MailProperties.INBOUND_REPLY_TO_ADDRESSES_PROPERTY,
                MailUtils.mailAddressesToString(mailMessage.getReplyTo()));
        }
        catch (MessagingException me)
        {
            log.warn("Invalid address found in ReplyTo header:", me);
        }

        try
        {
            muleMessage.setProperty(MailProperties.INBOUND_FROM_ADDRESS_PROPERTY,
                MailUtils.mailAddressesToString(mailMessage.getFrom()));
        }
        catch (javax.mail.MessagingException me)
        {
            log.warn("Invalid address found in From header:", me);
        }

        muleMessage.setProperty(MailProperties.INBOUND_SUBJECT_PROPERTY, 
            StringUtils.defaultIfEmpty(mailMessage.getSubject(), "(no subject)"));
        muleMessage.setProperty(MailProperties.INBOUND_CONTENT_TYPE_PROPERTY, 
            StringUtils.defaultIfEmpty(mailMessage.getContentType(), "text/plain"));

        Date sentDate = mailMessage.getSentDate();
        if (sentDate == null)
        {
            sentDate = new Date();
        }
        muleMessage.setProperty(MailProperties.SENT_DATE_PROPERTY, sentDate);

        for (Enumeration<?> e = mailMessage.getAllHeaders(); e.hasMoreElements();)
        {
            Header header = (Header) e.nextElement();
            
            String name = header.getName();
            String listName = MailUtils.toListHeader(name);
            String value = header.getValue();

            if (null == muleMessage.getProperty(name))
            {
                muleMessage.setProperty(name, value);
            }

            Object listPropertyValue = muleMessage.getProperty(listName);
            if (null == listPropertyValue)
            {
                listPropertyValue = new LinkedList<Object>();
                muleMessage.setProperty(listName, listPropertyValue);
            }
            if (listPropertyValue instanceof List<?>)
            {
                ((List) listPropertyValue).add(header.getValue());
            }
        }
    }

    @Override
    protected void addAttachments(MuleMessage muleMessage, Object transportMessage) throws Exception
    {
        super.addAttachments(muleMessage, transportMessage);

        Object content = ((Message) transportMessage).getContent();
        if (content instanceof Multipart)
        {
            Multipart multipart = (Multipart) content;
            
            TreeMap<String, Part> attachments = new TreeMap<String, Part>();
            MailUtils.getAttachments(multipart, attachments);

            log.debug("Received Multipart message. Adding attachments");
            for (Map.Entry<String, Part> entry : attachments.entrySet())
            {
                Part part = entry.getValue();
                String name = entry.getKey().toString();

                muleMessage.addAttachment(name, part.getDataHandler());
                addAttachmentHeaders(name, part, muleMessage);
            }
        }
    }
    
    protected void addAttachmentHeaders(String name, Part part, MuleMessage muleMessage) throws javax.mail.MessagingException
    {
        Map<String, String> headers = new HashMap<String, String>();
        for (Enumeration<?> e = part.getAllHeaders(); e.hasMoreElements();)
        {
            Header h = (Header) e.nextElement();
            headers.put(h.getName(), h.getValue());
        }
        
        if (headers.size() > 0)
        {
            muleMessage.setProperty(
                name + AbstractMailConnector.ATTACHMENT_HEADERS_PROPERTY_POSTFIX, headers);
        }
    }
}
