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

import java.util.Calendar;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;

import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.StringUtils;

/**
 * <code>SmtpMessageDispatcher</code> will dispatch Mule events as Mime email
 * messages over an SMTP gateway
 */
public class SmtpMessageDispatcher extends AbstractMessageDispatcher
{
    private final SmtpConnector connector;
    protected volatile Transport transport;
    protected volatile Session session;

    public SmtpMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (SmtpConnector)endpoint.getConnector();
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception
    {
        if (transport == null)
        {
            UMOEndpointURI uri = endpoint.getEndpointURI();

            // Try to get the properties from the endpoint and use the connector
            // properties if they are not given.

            String host = uri.getHost();
            if (host == null)
            {
                host = connector.getHost();
            }

            int port = uri.getPort();
            if (port == -1)
            {
                port = connector.getPort();
            }

            String username = uri.getUsername();
            if (StringUtils.isBlank(username))
            {
                username = connector.getUsername();
            }

            String password = uri.getPassword();
            if (StringUtils.isBlank(password))
            {
                password = connector.getPassword();
            }

            URLName url = new URLName(connector.getProtocol(), host, port, null, username, password);

            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Creating mail session, host = " + url.getHost() + ", port = "
                                    + url.getPort() + ", user = " + url.getUsername() + ", pass = "
                                    + url.getPassword());
                }
                session = MailUtils.createMailSession(url, connector);
                session.setDebug(logger.isDebugEnabled());

                transport = session.getTransport(url);
                transport.connect(uri.getHost(), uri.getPort(), uri.getUsername(), uri.getPassword());
            }
            catch (Exception e)
            {
                throw new EndpointException(
                    org.mule.config.i18n.Message.createStaticMessage("Unable to connect to mail transport."),
                    e);
            }
        }
    }

    protected void doDisconnect() throws Exception
    {
        try
        {
            transport.close();
        }
        finally
        {
            transport = null;
            session = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOConnector#dispatch(java.lang.Object,
     *      org.mule.providers.MuleEndpoint)
     */
    public void doDispatch(UMOEvent event)
    {
        try
        {
            Object data = event.getTransformedMessage();

            if (!(data instanceof Message))
            {
                throw new DispatchException(new org.mule.config.i18n.Message(
                    Messages.TRANSFORM_X_UNEXPECTED_TYPE_X, data.getClass().getName(),
                    Message.class.getName()), event.getMessage(), event.getEndpoint());
            }
            else
            {
                // Check the message for any unset data and use defaults
                sendMailMessage((Message)data);
            }
        }
        catch (Exception e)
        {
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

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception
    {
        throw new UnsupportedOperationException("doReceive");
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return event.getMessage();
    }

    protected void sendMailMessage(Message message) throws MessagingException
    {
        // sent date
        message.setSentDate(Calendar.getInstance().getTime());

        // These getAllRecipients() and similar methods always return null???
        // Seems like JavaMail 1.3.3 is setting headers only
        // transport.sendMessage(message, message.getAllRecipients());

        // this call at least preserves the TO field
        // TODO handle CC and BCC
        Transport.send(message);
        if (logger.isDebugEnabled())
        {
            StringBuffer msg = new StringBuffer(200);
            msg.append("Email message sent with subject'").append(message.getSubject()).append("' sent- ");
            msg.append(", From: ").append(MailUtils.mailAddressesToString(message.getFrom())).append(" ");
            msg.append(", To: ").append(
                MailUtils.mailAddressesToString(message.getRecipients(Message.RecipientType.TO))).append(" ");
            msg.append(", Cc: ").append(
                MailUtils.mailAddressesToString(message.getRecipients(Message.RecipientType.CC))).append(" ");
            msg.append(", Bcc: ")
                .append(MailUtils.mailAddressesToString(message.getRecipients(Message.RecipientType.BCC)))
                .append(" ");
            msg.append(", ReplyTo: ").append(MailUtils.mailAddressesToString(message.getReplyTo()));

            logger.debug(msg.toString());
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

    protected void doDispose()
    {
        session = null;
    }
}
