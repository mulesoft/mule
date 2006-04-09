/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */
package org.mule.providers.email.transformers;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.providers.email.MailProperties;
import org.mule.providers.email.MailUtils;
import org.mule.providers.email.SmtpConnector;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.PropertiesHelper;
import org.mule.util.TemplateParser;

/**
 * <code>StringToEmailMessage</code> will convert a string to a java mail
 * Message, using the string as the contents. This implementation uses
 * properties on the transformer to determine the to and subject fields.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class StringToEmailMessage extends AbstractEventAwareTransformer
{
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
        String cc = eventMsg.getStringProperty(MailProperties.CC_ADDRESSES_PROPERTY, connector.getCcAddresses());
        String bcc = eventMsg.getStringProperty(MailProperties.BCC_ADDRESSES_PROPERTY, connector.getBccAddresses());
        String from = eventMsg.getStringProperty(MailProperties.FROM_ADDRESS_PROPERTY, connector.getFromAddress());
        String replyTo = eventMsg.getStringProperty(MailProperties.REPLY_TO_ADDRESSES_PROPERTY, connector.getReplyToAddresses());
        String subject = eventMsg.getStringProperty(MailProperties.SUBJECT_PROPERTY, connector.getSubject());

        String contentType = eventMsg.getStringProperty(MailProperties.CONTENT_TYPE_PROPERTY, connector.getContentType());

        Properties headers = new Properties();
        Properties customHeaders = connector.getCustomHeaders();
        if(customHeaders != null && !customHeaders.isEmpty()) {
            headers.putAll(customHeaders);
        }
        Properties otherHeaders = (Properties)eventMsg.getProperty(MailProperties.CUSTOM_HEADERS_MAP_PROPERTY);
        if(otherHeaders != null && !otherHeaders.isEmpty()) {
            Map props = new HashMap(MuleManager.getInstance().getProperties());
            UMOMessage msg = context.getMessage();
            for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();) {
                Object propertyKeys = iterator.next();
                props.put(propertyKeys, context.getMessage().getProperty(propertyKeys));
            }
            headers.putAll(templateParser.parse(props, otherHeaders));
        }

        if(logger.isDebugEnabled()) {
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
            buf.append("Custom Headers: ").append(PropertiesHelper.propertiesToString(headers, false));
            logger.debug(buf.toString());
        }

        try {
            Message msg = new MimeMessage((Session) endpoint.getConnector().getDispatcher(endpoint).getDelegateSession());

            msg.setRecipients(Message.RecipientType.TO, MailUtils.stringToInternetAddresses(to));

            // sent date
            msg.setSentDate(Calendar.getInstance().getTime());

            if (StringUtils.isNotBlank(from)) {
                msg.setFrom(MailUtils.stringToInternetAddresses(from)[0]);
            }

            if (StringUtils.isNotBlank(cc)) {
                msg.setRecipients(Message.RecipientType.CC, MailUtils.stringToInternetAddresses(cc));
            }

            if (StringUtils.isNotBlank(bcc)) {
                msg.setRecipients(Message.RecipientType.BCC, MailUtils.stringToInternetAddresses(bcc));
            }

            if (StringUtils.isNotBlank(replyTo)) {
                eventMsg.setReplyTo(MailUtils.stringToInternetAddresses(replyTo));
            }

            msg.setSubject(subject);

            Map.Entry entry;
            for (Iterator iterator = headers.entrySet().iterator(); iterator.hasNext();) {
                entry = (Map.Entry)iterator.next();
                msg.setHeader(entry.getKey().toString(), entry.getValue().toString());
            }

            setContent(src, msg, contentType, context);

            return msg;
        } catch (Exception e) {
            throw new TransformerException(this, e);
        }
    }

    protected void setContent(Object payload, Message msg, String contentType, UMOEventContext context) throws Exception {
        msg.setContent(payload, contentType);
    }

}
