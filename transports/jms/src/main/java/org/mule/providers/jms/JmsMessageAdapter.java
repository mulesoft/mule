/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.config.MuleProperties;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import java.util.Enumeration;

/**
 * <code>JmsMessageAdapter</code> allows a <code>MuleEvent</code> to access
 * the properties and payload of a JMS Message in a uniform way. The
 * JmsMessageAdapter expects a message of type <i>javax.jms.Message</i> and
 * will throw an IllegalArgumentException if the source message type is not
 * compatible. The JmsMessageAdapter should be suitable for all JMS Connector
 * implementations.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JmsMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -5979930419887129835L;

    private Message message = null;

    public JmsMessageAdapter(Object message) throws MessagingException
    {
        setMessage(message);
    }

    /**
     * Converts the message implementation into a String representation
     *
     * @param encoding The encoding to use when transforming the message (if necessary). The parameter is
     *                 used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception {
        return new String(getPayloadAsBytes(), encoding);
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return JmsMessageUtils.getBytesFromMessage(message);
    }

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return message;
    }

    /**
     * @param message new value for the message
     */
    private void setMessage(Object message) throws MessagingException
    {
        if (message instanceof Message) {
            this.message = (Message) message;
        } else {
            throw new MessageTypeNotSupportedException(message, getClass());
        }

        try {
            String value =  this.message.getJMSCorrelationID();
            if (value != null) {
                setProperty(JmsConstants.JMS_CORRELATION_ID, value);
            }
        } catch (JMSException e) {
            // ignored
        }

        try {
            int value = this.message.getJMSDeliveryMode();
            setProperty(JmsConstants.JMS_DELIVERY_MODE, new Integer(value));
        } catch (JMSException e) {
            // ignored
        }

        try {
            Destination value = this.message.getJMSDestination();
            if (value != null) {
                setProperty(JmsConstants.JMS_DESTINATION, value);
            }
        } catch (JMSException e) {
            // ignored
        }

        try {
            long value = this.message.getJMSExpiration();
            setProperty(JmsConstants.JMS_EXPIRATION, new Long(value));
        } catch (JMSException e) {
            // ignored
        }

        try {
            String value = this.message.getJMSMessageID();
            if (value != null) {
                setProperty(JmsConstants.JMS_MESSAGE_ID, value);
            }
        } catch (JMSException e) {
            // ignored
        }

        try {
            int value = this.message.getJMSPriority();
            setProperty(JmsConstants.JMS_PRIORITY, new Integer(value));
        } catch (JMSException e) {
            // ignored
        }

        try {
            boolean value = this.message.getJMSRedelivered();
            setProperty(JmsConstants.JMS_REDELIVERED, Boolean.valueOf(value));
        } catch (JMSException e) {
            // ignored
        }

        try {
            Destination value = this.message.getJMSReplyTo();
            if (value != null) {
                setProperty(JmsConstants.JMS_REPLY_TO, value);
            }
        } catch (JMSException e) {
            // ignored
        }

        try {
            long value = this.message.getJMSTimestamp();
            setProperty(JmsConstants.JMS_TIMESTAMP, new Long(value));
        } catch (JMSException e) {
            // ignored
        }

        try {
            String value = this.message.getJMSType();
            if (value != null) {
                setProperty(JmsConstants.JMS_TYPE, value);
            }
        } catch (JMSException e) {
            // ignored
        }

        try {
            Enumeration e = this.message.getPropertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                try {
                    Object value = this.message.getObjectProperty(key);
                    if (value != null) {
                        setProperty(key, value);
                    }
                } catch (JMSException e1) {
                    // ignored
                }
            }
        } catch (JMSException e1) {
            // ignored
        }
    }

    public String getUniqueId() {
        return (String)getProperty(JmsConstants.JMS_MESSAGE_ID);
    }

    /**
     * Sets a correlationId for this message. The correlation Id can be used by
     * components in the system to manage message relations <p/> transport
     * protocol. As such not all messages will support the notion of a
     * correlationId i.e. tcp or file. In this situation the correlation Id is
     * set as a property of the message where it's up to developer to keep the
     * association with the message. For example if the message is serialised to
     * xml the correlationId will be available in the message.
     * 
     * @param id the Id reference for this relationship
     */
    public void setCorrelationId(String id)
    {
        setProperty(JmsConstants.JMS_CORRELATION_ID, id);
    }

    /**
     * Sets a correlationId for this message. The correlation Id can be used by
     * components in the system to manage message relations. <p/> The
     * correlationId is associated with the message using the underlying
     * transport protocol. As such not all messages will support the notion of a
     * correlationId i.e. tcp or file. In this situation the correlation Id is
     * set as a property of the message where it's up to developer to keep the
     * association with the message. For example if the message is serialised to
     * xml the correlationId will be available in the message.
     * 
     * @return the correlationId for this message or null if one hasn't been set
     */
    public String getCorrelationId()
    {
        return (String)getProperty(JmsConstants.JMS_CORRELATION_ID);
    }

    /**
     * Sets a replyTo address for this message. This is useful in an
     * asynchronous environment where the caller doesn't wait for a response and
     * the response needs to be routed somewhere for further processing. The
     * value of this field can be any valid endpointUri url.
     * 
     * @param replyTo the endpointUri url to reply to
     */
     public void setReplyTo(Object replyTo)
     {
        if(replyTo instanceof Destination) {
            setProperty(JmsConstants.JMS_REPLY_TO, replyTo);
        } else {
            super.setReplyTo(replyTo);
        }
     }
    /**
     * Sets a replyTo address for this message. This is useful in an
     * asynchronous environment where the caller doesn't wait for a response and
     * the response needs to be routed somewhere for further processing. The
     * value of this field can be any valid endpointUri url.
     * 
     * @return the endpointUri url to reply to or null if one has not been set
     */
    public Object getReplyTo()
    {
        Object replyTo = getProperty(JmsConstants.JMS_REPLY_TO);
        if (replyTo == null) {
            replyTo = getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
        }
        return replyTo;
    }

}
