/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
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

        addRecipientProperty(muleMessage, mailMessage, RecipientType.TO, MailProperties.TO_ADDRESSES_PROPERTY);
        addRecipientProperty(muleMessage, mailMessage, RecipientType.CC, MailProperties.CC_ADDRESSES_PROPERTY);
        addRecipientProperty(muleMessage, mailMessage, RecipientType.BCC, MailProperties.BCC_ADDRESSES_PROPERTY);

        addReplyToProperty(muleMessage, mailMessage);
        addFromProperty(muleMessage, mailMessage);

        muleMessage.setInboundProperty(MailProperties.SUBJECT_PROPERTY,
            StringUtils.defaultIfEmpty(mailMessage.getSubject(), "(no subject)"));
        muleMessage.setInboundProperty(MailProperties.CONTENT_TYPE_PROPERTY,
            StringUtils.defaultIfEmpty(mailMessage.getContentType(), "text/plain"));

        // TODO Remove in Mule 3.4
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
            muleMessage.setProperty(property, MailUtils.mailAddressesToString(recipients), PropertyScope.INBOUND);
            // TODO Remove in Mule 3.4
            muleMessage.setOutboundProperty(MailProperties.INBOUND_PREFIX + property, MailUtils.mailAddressesToString(recipients));
        }
        catch (MessagingException e)
        {
            if (mimeMessage != null)
            {
                String[] header = mimeMessage.getHeader(recipientType.toString());
                String recipients = StringUtils.join(header, ", ");
                muleMessage.setProperty(property, recipients, PropertyScope.INBOUND);
                // TODO Remove in Mule 3.4
                muleMessage.setOutboundProperty(MailProperties.INBOUND_PREFIX + property, recipients);
            }
        }
    }

    protected void addReplyToProperty(DefaultMuleMessage muleMessage, Message mailMessage)
    {
        try
        {
            muleMessage.setInboundProperty(MailProperties.REPLY_TO_ADDRESSES_PROPERTY,
                MailUtils.mailAddressesToString(mailMessage.getReplyTo()));
            // TODO Remove in Mule 3.4
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
            muleMessage.setInboundProperty(MailProperties.FROM_ADDRESS_PROPERTY,
                MailUtils.mailAddressesToString(mailMessage.getFrom()));
            // TODO Remove in Mule 3.4
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
        muleMessage.setInboundProperty(MailProperties.SENT_DATE_PROPERTY, sentDate);
        // TODO Remove in Mule 3.4
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
            muleMessage.setProperty(name + AbstractMailConnector.ATTACHMENT_HEADERS_PROPERTY_POSTFIX,
                headers, PropertyScope.INBOUND);
            // TODO Remove in Mule 3.4
            muleMessage.setOutboundProperty(name + AbstractMailConnector.ATTACHMENT_HEADERS_PROPERTY_POSTFIX,
                headers);
        }
    }
}
