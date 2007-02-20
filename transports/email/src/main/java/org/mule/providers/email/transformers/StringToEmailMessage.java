/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.transformers;

import org.mule.providers.email.MailProperties;
import org.mule.providers.email.MailUtils;
import org.mule.providers.email.SmtpConnector;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.MapUtils;
import org.mule.util.TemplateParser;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>StringToEmailMessage</code> will convert a String to a JavaMail Message,
 * using the String as the contents. This implementation uses properties on the
 * transformer to determine the To: and Subject: fields.
 */
public class StringToEmailMessage extends AbstractEventAwareTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2687301871292220521L;

    /**
     * logger used by this class
     */
    protected final transient Log logger = LogFactory.getLog(getClass());

    protected TemplateParser templateParser = TemplateParser.createAntStyleParser();

    public StringToEmailMessage()
    {
        registerSourceType(String.class);
        setReturnClass(Message.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object transform(Object src, String encoding, UMOEventContext context) throws TransformerException
    {
        String endpointAddress = endpoint.getEndpointURI().getAddress();
        SmtpConnector connector = (SmtpConnector)endpoint.getConnector();
        UMOMessage eventMsg = context.getMessage();
        String to = eventMsg.getStringProperty(MailProperties.TO_ADDRESSES_PROPERTY, endpointAddress);
        String cc = eventMsg.getStringProperty(MailProperties.CC_ADDRESSES_PROPERTY,
            connector.getCcAddresses());
        String bcc = eventMsg.getStringProperty(MailProperties.BCC_ADDRESSES_PROPERTY,
            connector.getBccAddresses());
        String from = eventMsg.getStringProperty(MailProperties.FROM_ADDRESS_PROPERTY,
            connector.getFromAddress());
        String replyTo = eventMsg.getStringProperty(MailProperties.REPLY_TO_ADDRESSES_PROPERTY,
            connector.getReplyToAddresses());
        String subject = eventMsg.getStringProperty(MailProperties.SUBJECT_PROPERTY, connector.getSubject());
        String contentType = eventMsg.getStringProperty(MailProperties.CONTENT_TYPE_PROPERTY,
            connector.getContentType());

        Properties headers = new Properties();
        Properties customHeaders = connector.getCustomHeaders();

        if (customHeaders != null && !customHeaders.isEmpty())
        {
            headers.putAll(customHeaders);
        }

        Properties otherHeaders = (Properties)eventMsg.getProperty(MailProperties.CUSTOM_HEADERS_MAP_PROPERTY);
        if (otherHeaders != null && !otherHeaders.isEmpty())
        {
            Map props = new HashMap(context.getManagmentContext().getProperties());
            for (Iterator iterator = eventMsg.getPropertyNames().iterator(); iterator.hasNext();)
            {
                String propertyKey = (String)iterator.next();
                props.put(propertyKey, eventMsg.getProperty(propertyKey));
            }
            headers.putAll(templateParser.parse(props, otherHeaders));
        }

        if (logger.isDebugEnabled())
        {
            StringBuffer buf = new StringBuffer(256);
            buf.append("Constructing email using:\n");
            buf.append("To: ").append(to);
            buf.append("From: ").append(from);
            buf.append("CC: ").append(cc);
            buf.append("BCC: ").append(bcc);
            buf.append("Subject: ").append(subject);
            buf.append("ReplyTo: ").append(replyTo);
            buf.append("Content type: ").append(contentType);
            buf.append("Payload type: ").append(src.getClass().getName());
            buf.append("Custom Headers: ").append(MapUtils.toString(headers, false));
            logger.debug(buf.toString());
        }

        try
        {
            Message email = new MimeMessage(((SmtpConnector)endpoint.getConnector()).getMailSession(endpoint));

            email.setRecipients(Message.RecipientType.TO, MailUtils.stringToInternetAddresses(to));

            // sent date
            email.setSentDate(Calendar.getInstance().getTime());

            if (StringUtils.isNotBlank(from))
            {
                email.setFrom(MailUtils.stringToInternetAddresses(from)[0]);
            }

            if (StringUtils.isNotBlank(cc))
            {
                email.setRecipients(Message.RecipientType.CC, MailUtils.stringToInternetAddresses(cc));
            }

            if (StringUtils.isNotBlank(bcc))
            {
                email.setRecipients(Message.RecipientType.BCC, MailUtils.stringToInternetAddresses(bcc));
            }

            if (StringUtils.isNotBlank(replyTo))
            {
                email.setReplyTo(MailUtils.stringToInternetAddresses(replyTo));
            }

            email.setSubject(subject);

            for (Iterator iterator = headers.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry entry = (Map.Entry)iterator.next();
                email.setHeader(entry.getKey().toString(), entry.getValue().toString());
            }

            setContent(src, email, contentType, context);

            return email;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected void setContent(Object payload, Message msg, String contentType, UMOEventContext context)
        throws Exception
    {
        msg.setContent(payload, contentType);
    }

}
