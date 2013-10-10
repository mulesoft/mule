/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import org.mule.DefaultMuleMessage;
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

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;

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
    protected void addProperties(DefaultMuleMessage muleMessage, Object transportMessage) throws Exception
    {
        super.addProperties(muleMessage, transportMessage);

        Message mailMessage = (Message) transportMessage;

        addRecipientProperty(muleMessage, mailMessage, RecipientType.TO, MailProperties.INBOUND_TO_ADDRESSES_PROPERTY);
        addRecipientProperty(muleMessage, mailMessage, RecipientType.CC, MailProperties.INBOUND_CC_ADDRESSES_PROPERTY);
        addRecipientProperty(muleMessage, mailMessage, RecipientType.BCC, MailProperties.INBOUND_BCC_ADDRESSES_PROPERTY);

        addReplyToProperty(muleMessage, mailMessage);
        addFromProperty(muleMessage, mailMessage);

        muleMessage.setOutboundProperty(MailProperties.INBOUND_SUBJECT_PROPERTY,
                                        StringUtils.defaultIfEmpty(mailMessage.getSubject(), "(no subject)"));
        muleMessage.setOutboundProperty(MailProperties.INBOUND_CONTENT_TYPE_PROPERTY,
                                        StringUtils.defaultIfEmpty(mailMessage.getContentType(), "text/plain"));

        addSentDateProperty(muleMessage, mailMessage);
        addMailHeadersToMessageProperties(mailMessage, muleMessage);
    }

    protected void addRecipientProperty(MuleMessage muleMessage, Message mailMessage,
        RecipientType recipientType, String property) throws MessagingException
    {
        MimeMessage mimeMessage = null;
        if (mailMessage instanceof MimeMessage)
        {
            mimeMessage = (MimeMessage) mailMessage;
        }

        try
        {
            Address[] recipients = mailMessage.getRecipients(recipientType);
            muleMessage.setOutboundProperty(property, MailUtils.mailAddressesToString(recipients));
        }
        catch (MessagingException e)
        {
            if (mimeMessage != null)
            {
                String[] header = mimeMessage.getHeader(recipientType.toString());
                String recipients = StringUtils.join(header, ", ");
                muleMessage.setOutboundProperty(property, recipients);
            }
        }
    }

    protected void addReplyToProperty(DefaultMuleMessage muleMessage, Message mailMessage)
    {
        try
        {
            muleMessage.setOutboundProperty(MailProperties.INBOUND_REPLY_TO_ADDRESSES_PROPERTY,
                                            MailUtils.mailAddressesToString(mailMessage.getReplyTo()));
        }
        catch (MessagingException me)
        {
            log.warn("Invalid address found in ReplyTo header:", me);
        }
    }

    protected void addFromProperty(DefaultMuleMessage muleMessage, Message mailMessage)
    {
        try
        {
            muleMessage.setOutboundProperty(MailProperties.INBOUND_FROM_ADDRESS_PROPERTY,
                                            MailUtils.mailAddressesToString(mailMessage.getFrom()));
        }
        catch (javax.mail.MessagingException me)
        {
            log.warn("Invalid address found in From header:", me);
        }
    }

    protected void addSentDateProperty(DefaultMuleMessage muleMessage, Message mailMessage)
        throws MessagingException
    {
        Date sentDate = mailMessage.getSentDate();
        if (sentDate == null)
        {
            sentDate = new Date();
        }
        muleMessage.setOutboundProperty(MailProperties.SENT_DATE_PROPERTY, sentDate);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void addMailHeadersToMessageProperties(Message mailMessage, DefaultMuleMessage muleMessage)
        throws MessagingException
    {
        for (Enumeration<?> e = mailMessage.getAllHeaders(); e.hasMoreElements();)
        {
            Header header = (Header) e.nextElement();

            String name = header.getName();
            String listName = MailUtils.toListHeader(name);
            String value = header.getValue();

            if (null == muleMessage.getOutboundProperty(name))
            {
                muleMessage.setOutboundProperty(name, value);
            }

            Object listPropertyValue = muleMessage.getOutboundProperty(listName);
            if (null == listPropertyValue)
            {
                listPropertyValue = new LinkedList<Object>();
                muleMessage.setOutboundProperty(listName, listPropertyValue);
            }
            if (listPropertyValue instanceof List<?>)
            {
                ((List) listPropertyValue).add(header.getValue());
            }
        }
    }

    @Override
    protected void addAttachments(DefaultMuleMessage muleMessage, Object transportMessage) throws Exception
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
                String name = entry.getKey();

                muleMessage.addInboundAttachment(name, part.getDataHandler());
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
            muleMessage.setOutboundProperty(name + AbstractMailConnector.ATTACHMENT_HEADERS_PROPERTY_POSTFIX,
                                            headers);
        }
    }
}
