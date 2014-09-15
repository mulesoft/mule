/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.email.MailProperties;
import org.mule.transport.email.MailUtils;
import org.mule.transport.email.SmtpConnector;
import org.mule.util.MapUtils;
import org.mule.util.StringUtils;
import org.mule.util.TemplateParser;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>StringToEmailMessage</code> will convert a String to a JavaMail Message,
 * using the String as the contents. This implementation uses properties on the
 * transformer to determine the To: and Subject: fields.
 */
public class StringToEmailMessage extends AbstractMessageTransformer
{
    /**
     * logger used by this class
     */
    private final Log logger = LogFactory.getLog(getClass());

    private TemplateParser templateParser = TemplateParser.createMuleStyleParser();

    public StringToEmailMessage()
    {
        this.registerSourceType(DataTypeFactory.STRING);
        this.setReturnDataType(DataTypeFactory.create(Message.class));
    }


    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        String endpointAddress = endpoint.getEndpointURI().getAddress();
        SmtpConnector connector = (SmtpConnector) endpoint.getConnector();
        String to = lookupProperty(message, MailProperties.TO_ADDRESSES_PROPERTY, endpointAddress);
        String cc = lookupProperty(message, MailProperties.CC_ADDRESSES_PROPERTY, connector.getCcAddresses());
        String bcc = lookupProperty(message, MailProperties.BCC_ADDRESSES_PROPERTY, connector.getBccAddresses());
        String from = lookupProperty(message, MailProperties.FROM_ADDRESS_PROPERTY, connector.getFromAddress());
        String replyTo = lookupProperty(message, MailProperties.REPLY_TO_ADDRESSES_PROPERTY, connector.getReplyToAddresses());
        String subject = lookupProperty(message, MailProperties.SUBJECT_PROPERTY, connector.getSubject());
        String contentType = lookupProperty(message, MailProperties.CONTENT_TYPE_PROPERTY, connector.getContentType());

        Properties headers = new Properties();
        Properties customHeaders = connector.getCustomHeaders();

        if (customHeaders != null && !customHeaders.isEmpty())
        {
            headers.putAll(customHeaders);
        }

        Properties otherHeaders = message.getOutboundProperty(MailProperties.CUSTOM_HEADERS_MAP_PROPERTY);
        if (otherHeaders != null && !otherHeaders.isEmpty())
        {
                //TODO Whats going on here?
//                final MuleContext mc = context.getMuleContext();
//                for (Iterator iterator = message.getPropertyNames().iterator(); iterator.hasNext();)
//                {
//                    String propertyKey = (String) iterator.next();
//                    mc.getRegistry().registerObject(propertyKey, message.getProperty(propertyKey), mc);
//                }
                headers.putAll(templateParser.parse(new TemplateParser.TemplateCallback()
                {
                    public Object match(String token)
                    {
                        return muleContext.getRegistry().lookupObject(token);
                    }
                }, otherHeaders));

        }

        if (logger.isDebugEnabled())
        {
            StringBuilder buf = new StringBuilder();
            buf.append("Constructing email using:\n");
            buf.append("To: ").append(to);
            buf.append(", From: ").append(from);
            buf.append(", CC: ").append(cc);
            buf.append(", BCC: ").append(bcc);
            buf.append(", Subject: ").append(subject);
            buf.append(", ReplyTo: ").append(replyTo);
            buf.append(", Content type: ").append(contentType);
            buf.append(", Payload type: ").append(message.getPayload().getClass().getName());
            buf.append(", Custom Headers: ").append(MapUtils.toString(headers, false));
            logger.debug(buf.toString());
        }

        try
        {
            MimeMessage email = new MimeMessage(((SmtpConnector) endpoint.getConnector()).getSessionDetails(endpoint).getSession());

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

            email.setSubject(subject, outputEncoding);

            for (Iterator iterator = headers.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry entry = (Map.Entry) iterator.next();
                email.setHeader(entry.getKey().toString(), entry.getValue().toString());
            }

            setContent(message.getPayload(), email, contentType + "; charset=" + outputEncoding, message);

            return email;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    /**
     * Searches in outbound, then invocation scope. If not found, returns a passed in default value.
     */
    protected String lookupProperty(MuleMessage message, String propName, String defaultValue)
    {
        String value = message.getOutboundProperty(propName);
        if (value == null)
        {
            value = message.getInvocationProperty(propName, defaultValue);
        }
        return evaluate(value, message);
    }

    public String evaluate(String value, MuleMessage message)
    {
        if(value != null && muleContext.getExpressionManager().isExpression(value))
        {
            value = (String) muleContext.getExpressionManager().evaluate(value, message);
        }
        return value;
    }

    protected void setContent(Object payload, Message msg, String contentType, MuleMessage message)
        throws Exception
    {
        msg.setContent(payload, contentType);
    }

}
