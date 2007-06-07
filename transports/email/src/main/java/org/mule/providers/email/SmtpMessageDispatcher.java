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

import org.mule.config.i18n.CoreMessages;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;

import java.util.Calendar;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

/**
 * <code>SmtpMessageDispatcher</code> will dispatch Mule events as Mime email
 * messages over an SMTP gateway.
 * 
 * This contains a reference to a transport (and endpoint and connector, via superclasses)
 */
public class SmtpMessageDispatcher extends AbstractMessageDispatcher
{
    private volatile Transport transport;

    public SmtpMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    private SmtpConnector castConnector()
    {
        return (SmtpConnector) getConnector();
    }

    protected void doConnect() throws Exception
    {
        if (transport == null)
        {
            try
            {
                transport = castConnector().getSessionDetails(endpoint).newTransport();
                UMOEndpointURI uri = endpoint.getEndpointURI();
                transport.connect(uri.getHost(), uri.getPort(), uri.getUsername(), uri.getPassword());
            }
            catch (Exception e)
            {
                throw new EndpointException(
                    org.mule.config.i18n.MessageFactory.createStaticMessage("Unable to connect to mail transport."),
                    e);
            }
        }
    }

    protected void doDisconnect() throws Exception
    {
        if (null != transport)
        {
            try
            {
                transport.close();
            }
            finally
            {
                transport = null;
            }
        }
    }

    protected void doDispatch(UMOEvent event)
    {
        try
        {
            Object data = event.getTransformedMessage();

            if (!(data instanceof Message))
            {
                throw new DispatchException(
                    CoreMessages.transformUnexpectedType(data.getClass(), Message.class),
                    event.getMessage(), event.getEndpoint());
            }
            else
            {
                // Check the message for any unset data and use defaults
                sendMailMessage((Message) data);
            }
        }
        catch (Exception e)
        {
            connector.handleException(e);
        }
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(long timeout) throws Exception
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

        /*
         * Double check that the transport is still connected as some SMTP servers may 
         * disconnect idle connections.
         */
        if (!transport.isConnected())
        {
            UMOEndpointURI uri = endpoint.getEndpointURI();
            if (logger.isInfoEnabled())
            {
                logger.info("Connection closed by remote server. Reconnecting.");
            }
            transport.connect(uri.getHost(), uri.getPort(), uri.getUsername(), uri.getPassword());
        }

        transport.sendMessage(message, message.getAllRecipients());

        if (logger.isDebugEnabled())
        {
            StringBuffer msg = new StringBuffer();
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

    protected void doDispose()
    {
        // nothing doing
    }

}
