/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.mule.api.MessagingException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.AbstractMessageAdapter;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

/**
 * <code>JmsMessageAdapter</code> allows a <code>DefaultMuleEvent</code> to access the
 * properties and payload of a JMS Message in a uniform way. The JmsMessageAdapter
 * expects a message of type <i>javax.jms.Message</i> and will throw an
 * IllegalArgumentException if the source message type is not compatible. The
 * JmsMessageAdapter should be suitable for all JMS Connector implementations.
 */
public class JmsMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8151716840620558143L;

    private String jmsSpec;
    private Message jmsMessage;

    public JmsMessageAdapter(Object message) throws MessagingException
    {
        super();
        this.setMessage(message);
    }

    protected JmsMessageAdapter(JmsMessageAdapter template)
    {
        super(template);
        jmsSpec = template.jmsSpec;
        jmsMessage = template.jmsMessage;
    }

    public void setSpecification(String newSpec)
    {
        if (JmsConstants.JMS_SPECIFICATION_11.equals(newSpec)
                || (JmsConstants.JMS_SPECIFICATION_102B.equals(newSpec)))
        {
            this.jmsSpec = newSpec;
        }
        else

        {
            throw new IllegalArgumentException(
                    "JMS specification needs to be one of the defined values in JmsConstants but was: " + newSpec);
        }
    }

    /**
     * Converts the message implementation into a String representation
     *
     * @param encoding The encoding to use when transforming the message (if
     *                 necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
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
        return JmsMessageUtils.toByteArray(jmsMessage, jmsSpec, getEncoding());
    }

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return jmsMessage;
    }

    /**
     * Decomposes the received message into a payload, properties (headers) and possibly attachements too.
     * Important note: when adding properties you must assign them to the inbound scope.  this can be done in
     * two ways-
     * <ol>
     * <li>use the method {@link #setProperty(String, Object, org.mule.api.transport.PropertyScope)} using the
     * {@link org.mule.api.transport.PropertyScope.INBOUND}</li>
     * <li>use the {@link #addInboundProperties(java.util.Map)} method to add all inbound properties at once.
     * </ol>
     * 
     * @param message the message received by the Mule endpoint in it's raw form.
     */
    private void setMessage(Object message) throws MessagingException
    {
        Map props = new HashMap();
        if (message instanceof Message)
        {
            this.jmsMessage = (Message) message;
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }

        try
        {
            String value = this.jmsMessage.getJMSCorrelationID();
            if (value != null)
            {
                props.put(JmsConstants.JMS_CORRELATION_ID, value);
                //Map to the internal Mule property
                props.put(MuleProperties.MULE_CORRELATION_ID_PROPERTY, value);
            }
        }
        catch (JMSException e)
        {
            // ignored
        }

        try
        {
            int value = this.jmsMessage.getJMSDeliveryMode();
            props.put(JmsConstants.JMS_DELIVERY_MODE, new Integer(value));
        }
        catch (JMSException e)
        {
            // ignored
        }

        try
        {
            Destination value = this.jmsMessage.getJMSDestination();
            if (value != null)
            {
                props.put(JmsConstants.JMS_DESTINATION, value);
            }
        }
        catch (JMSException e)
        {
            // ignored
        }

        try
        {
            long value = this.jmsMessage.getJMSExpiration();
            props.put(JmsConstants.JMS_EXPIRATION, new Long(value));
        }
        catch (JMSException e)
        {
            // ignored
        }

        try
        {
            String value = this.jmsMessage.getJMSMessageID();
            if (value != null)
            {
                props.put(JmsConstants.JMS_MESSAGE_ID, value);
                props.put(MuleProperties.MULE_MESSAGE_ID_PROPERTY, value);
            }
        }
        catch (JMSException e)
        {
            // ignored
        }

        try
        {
            int value = this.jmsMessage.getJMSPriority();
            props.put(JmsConstants.JMS_PRIORITY, new Integer(value));
        }
        catch (JMSException e)
        {
            // ignored
        }

        try
        {
            boolean value = this.jmsMessage.getJMSRedelivered();
            props.put(JmsConstants.JMS_REDELIVERED, Boolean.valueOf(value));
        }
        catch (JMSException e)
        {
            // ignored
        }

        try
        {
            Destination value = this.jmsMessage.getJMSReplyTo();
            if (value != null)
            {
                //Special handling of replyTo since it needs to go into the invocation scope
                setReplyTo(value);
            }
        }
        catch (JMSException e)
        {
            // ignored
        }

        try
        {
            long value = this.jmsMessage.getJMSTimestamp();
            props.put(JmsConstants.JMS_TIMESTAMP, new Long(value));
        }
        catch (JMSException e)
        {
            // ignored
        }

        try
        {
            String value = this.jmsMessage.getJMSType();
            if (value != null)
            {
                props.put(JmsConstants.JMS_TYPE, value);
            }
        }
        catch (JMSException e)
        {
            // ignored
        }

        try
        {
            Enumeration e = this.jmsMessage.getPropertyNames();
            while (e.hasMoreElements())
            {
                String key = (String) e.nextElement();
                try
                {
                    Object value = this.jmsMessage.getObjectProperty(key);
                    if (value != null)
                    {
                        props.put(key, value);
                    }
                }
                catch (JMSException e1)
                {
                    // ignored
                }
            }
        }
        catch (JMSException e1)
        {
            // ignored
        }
        addInboundProperties(props);
    }


    /**
     * Sets a correlationId for this message. The correlation Id can be used by
     * components in the system to manage message relations <p/> transport protocol.
     * As such not all messages will support the notion of a correlationId i.e. tcp
     * or file. In this situation the correlation Id is set as a property of the
     * message where it's up to developer to keep the association with the message.
     * For example if the message is serialised to xml the correlationId will be
     * available in the message.
     *
     * @param id the Id reference for this relationship
     */
    public void setCorrelationId(String id)
    {
        super.setCorrelationId(id);
        setProperty(JmsConstants.JMS_CORRELATION_ID, id);
    }


    /**
     * Sets a replyTo address for this message. This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response
     * needs to be routed somewhere for further processing. The value of this field
     * can be any valid endpointUri url.
     *
     * @param replyTo the endpointUri url to reply to
     */
    public void setReplyTo(Object replyTo)
    {
        if (replyTo instanceof Destination)
        {
            setProperty(JmsConstants.JMS_REPLY_TO, replyTo, PropertyScope.INVOCATION);
        }
        super.setReplyTo(replyTo);
    }

    public ThreadSafeAccess newThreadCopy()
    {
        return new JmsMessageAdapter(this);
    }

}
