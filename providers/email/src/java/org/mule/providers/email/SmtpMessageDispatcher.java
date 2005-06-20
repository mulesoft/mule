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
package org.mule.providers.email;

import java.util.Calendar;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;

import org.mule.MuleException;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.PropertiesHelper;

/**
 * @author Ross Mason
 */
public class SmtpMessageDispatcher extends AbstractMessageDispatcher
{
    private Session session;

    private SmtpConnector connector;

    /**
     * @param connector
     */
    public SmtpMessageDispatcher(SmtpConnector connector)
    {
        super(connector);
        this.connector = connector;

        URLName url = new URLName(connector.getProtocol(),
                                  connector.getHostname(),
                                  connector.getPort(),
                                  null,
                                  connector.getUsername(),
                                  connector.getPassword());
        session = MailUtils.createMailSession(url);
        session.setDebug(logger.isDebugEnabled());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOConnector#dispatch(java.lang.Object,
     *      org.mule.providers.MuleEndpoint)
     */
    public void doDispatch(UMOEvent event)
    {
        UMOEndpoint endpoint = event.getEndpoint();

        String endpointAddress = endpoint.getEndpointURI().getAddress();
        Map props = event.getProperties();
        String to = PropertiesHelper.getStringProperty(props, SmtpConnector.TO_ADDRESSES_PROPERTY, endpointAddress);
        String cc = PropertiesHelper.getStringProperty(props,
                                                       SmtpConnector.CC_ADDRESSES_PROPERTY,
                                                       connector.getCcAddresses());
        String bcc = PropertiesHelper.getStringProperty(props,
                                                        SmtpConnector.BCC_ADDRESSES_PROPERTY,
                                                        connector.getBccAddresses());
        String from = PropertiesHelper.getStringProperty(props,
                                                         SmtpConnector.FROM_ADDRESS_PROPERTY,
                                                         connector.getFromAddress());
        String subject = PropertiesHelper.getStringProperty(props,
                                                            SmtpConnector.SUBJECT_PROPERTY,
                                                            connector.getSubject());

        Message msg = null;

        try {
            Object data = event.getTransformedMessage();

            if (!(data instanceof Message)) {
                msg = connector.createMessage(from,
                                              to,
                                              cc,
                                              bcc,
                                              subject,
                                              data == null ? null : data.toString(),
                                              session);
            } else {
                // Check the message for any unset data and use defaults
                msg = (Message) data;

                if (msg.getRecipients(Message.RecipientType.TO) == null) {
                    // to
                    InternetAddress[] toAddrs = null;
                    toAddrs = InternetAddress.parse(to, false);
                    msg.setRecipients(Message.RecipientType.TO, toAddrs);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Sending message to: " + to);
                    }
                }

                // From
                if (msg.getFrom() == null) {
                    msg.setFrom(new InternetAddress(from));
                }

                // cc
                InternetAddress[] ccAddrs = null;
                if ((cc != null) && !cc.equals("")) {
                    ccAddrs = InternetAddress.parse(cc, false);
                    msg.setRecipients(Message.RecipientType.CC, ccAddrs);
                }

                // bcc
                InternetAddress[] bccAddrs = null;
                if ((bcc != null) && !bcc.equals("")) {
                    bccAddrs = InternetAddress.parse(bcc, false);
                    msg.setRecipients(Message.RecipientType.BCC, bccAddrs);
                }

                // Subjct
                if (msg.getSubject() == null || "".equals(msg.getSubject())) {
                    msg.setSubject(subject);
                }
            }

            sendMailMessage(msg);
            if (logger.isDebugEnabled()) {
                logger.debug("Sent message to: " + msg.getRecipients(Message.RecipientType.TO)[0].toString() + ", cc: "
                        + cc + ", bcc: " + bcc + ", from: " + from);
            }
        } catch (Exception e) {
            connector.handleException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#getDelegateSession()
     */
    public Object getDelegateSession() throws UMOException
    {
        return session;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#receive(java.lang.String,
     *      org.mule.umo.UMOEvent)
     */
    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        throw new UnsupportedOperationException("Cannot do a receive on an SmtpConnector");
    }

    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return event.getMessage();
    }

    protected void sendMailMessage(String to, String cc, String bcc, String subject, String body) throws MuleException,
            MessagingException
    {
        Message msg = connector.createMessage(connector.getFromAddress(), to, cc, bcc, subject, body, session);
        sendMailMessage(msg);
    }

    protected void sendMailMessage(Message message) throws MessagingException
    {
        // sent date
        message.setSentDate(Calendar.getInstance().getTime());
        Transport.send(message);
        if (logger.isInfoEnabled()) {
            StringBuffer msg = new StringBuffer();
            msg.append("Email message sent with subject'").append(message.getSubject()).append("' sent- ");
            String to = getRecipients(message, Message.RecipientType.TO);
            String cc = getRecipients(message, Message.RecipientType.CC);
            String bcc = getRecipients(message, Message.RecipientType.BCC);

            msg.append("FROM: ").append(message.getFrom()[0]).append(" ");
            if (to != null)
                msg.append("TO: ").append(to).append(" ");
            if (cc != null)
                msg.append("CC: ").append(cc).append(" ");
            if (bcc != null)
                msg.append("BCC: ").append(bcc).append(" ");

            logger.info(msg.toString());
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#getConnector()
     */
    public UMOConnector getConnector()
    {
        return connector;
    }

    public String getRecipients(Message message, Message.RecipientType type)
    {
        Address[] addresses = null;
        try {
            addresses = message.getRecipients(type);
        } catch (MessagingException e) {
            logger.error("Failed to get recipients from message: " + e.getMessage());
            return null;
        }
        if (addresses == null)
            return null;

        StringBuffer result = new StringBuffer();

        for (int i = 0; i < addresses.length; i++) {
            result.append(addresses[i] + ", ");
        }
        return result.substring(0, result.length() - 2);
    }

    public void doDispose()
    {
        session = null;
    }
}
