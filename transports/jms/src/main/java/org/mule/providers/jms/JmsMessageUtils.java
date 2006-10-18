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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageEOFException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.mule.util.ArrayUtils;

/**
 * <code>JmsMessageUtils</code> contains helper method for dealing with JMS
 * messages in Mule.
 */
public class JmsMessageUtils
{

    public static Message toMessage(Object object, Session session) throws JMSException
    {
        if (object instanceof Message)
        {
            return (Message)object;
        }
        else if (object instanceof String)
        {
            return session.createTextMessage((String)object);
        }
        else if (object instanceof Map)
        {
            MapMessage mMsg = session.createMapMessage();
            Map src = (Map)object;

            for (Iterator i = src.entrySet().iterator(); i.hasNext();)
            {
                Map.Entry entry = (Map.Entry)i.next();
                mMsg.setObject(entry.getKey().toString(), entry.getValue());
            }

            return mMsg;
        }
        else if (object instanceof InputStream)
        {
            StreamMessage sMsg = session.createStreamMessage();
            InputStream temp = (InputStream)object;

            byte[] buffer = new byte[4096];
            int len;

            try
            {
                while ((len = temp.read(buffer)) != -1)
                {
                    sMsg.writeBytes(buffer, 0, len);
                }
            }
            catch (IOException e)
            {
                throw new JMSException("Failed to read input stream to create a stream message: " + e);
            }

            return sMsg;
        }
        else if (object instanceof byte[])
        {
            BytesMessage bMsg = session.createBytesMessage();
            bMsg.writeBytes((byte[])object);
            return bMsg;
        }
        else if (object instanceof Serializable)
        {
            ObjectMessage oMsg = session.createObjectMessage();
            oMsg.setObject((Serializable)object);
            return oMsg;
        }
        else
        {
            throw new JMSException(
                "Source was not a supported type, data must be Serializable, String, byte[], Map or InputStream");
        }
    }

    public static Object toObject(Message source, String jmsSpec) throws JMSException, IOException
    {
        if (source instanceof ObjectMessage)
        {
            return ((ObjectMessage)source).getObject();
        }
        else if (source instanceof MapMessage)
        {
            Hashtable map = new Hashtable();
            MapMessage m = (MapMessage)source;

            for (Enumeration e = m.getMapNames(); e.hasMoreElements();)
            {
                String name = (String)e.nextElement();
                Object obj = m.getObject(name);
                map.put(name, obj);
            }

            return map;
        }
        else if (source instanceof TextMessage)
        {
            return ((TextMessage)source).getText();
        }
        else if (source instanceof BytesMessage)
        {
            return toByteArray(source, jmsSpec);
        }
        else if (source instanceof StreamMessage)
        {
            try
            {
                StreamMessage sMsg = (StreamMessage)source;
                Vector result = new Vector();
                Object obj;
                while ((obj = sMsg.readObject()) != null)
                {
                    result.addElement(obj);
                }
                return result;
            }
            catch (MessageEOFException eof)
            {
                // ignored
            }
            catch (Exception e)
            {
                throw new JMSException("Failed to extract information from JMS Stream Message: " + e);
            }
        }

        // what else is there to do?
        return source;
    }

    /**
     * @param message the message to receive the bytes from. Note this only works for
     *            TextMessge, ObjectMessage, StreamMessage and BytesMessage.
     * @param jmsSpec indicates the JMS API version, either
     *            {@link JmsConstants#JMS_SPECIFICATION_102B} or
     *            {@link JmsConstants#JMS_SPECIFICATION_11}. Any other value
     *            including <code>null</code> is treated as fallback to
     *            {@link JmsConstants#JMS_SPECIFICATION_102B}.
     * @return a byte array corresponding with the message payload
     * @throws JMSException if the message can't be read or if the message passed is
     *             a MapMessage
     * @throws java.io.IOException if a failure occurs while reading the stream and
     *             converting the message data
     */
    public static byte[] toByteArray(Message message, String jmsSpec) throws JMSException, IOException
    {
        if (message instanceof BytesMessage)
        {
            BytesMessage bMsg = (BytesMessage)message;
            bMsg.reset();

            if (JmsConstants.JMS_SPECIFICATION_11.equals(jmsSpec))
            {
                long bmBodyLength = bMsg.getBodyLength();
                if (bmBodyLength > Integer.MAX_VALUE)
                {
                    throw new JMSException("Size of BytesMessage exceeds Integer.MAX_VALUE; "
                                           + "please consider using JMS StreamMessage instead");
                }

                if (bmBodyLength > 0)
                {
                    byte[] bytes = new byte[(int)bmBodyLength];
                    bMsg.readBytes(bytes);
                    return bytes;
                }
                else
                {
                    return ArrayUtils.EMPTY_BYTE_ARRAY;
                }
            }
            else
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
                byte[] buffer = new byte[4096];
                int len;

                while ((len = bMsg.readBytes(buffer)) != -1)
                {
                    baos.write(buffer, 0, len);
                }

                if (baos.size() > 0)
                {
                    return baos.toByteArray();
                }
                else
                {
                    return ArrayUtils.EMPTY_BYTE_ARRAY;
                }
            }
        }
        else if (message instanceof StreamMessage)
        {
            StreamMessage sMsg = (StreamMessage)message;
            sMsg.reset();

            ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
            byte[] buffer = new byte[4096];
            int len;

            while ((len = sMsg.readBytes(buffer)) != -1)
            {
                baos.write(buffer, 0, len);
            }

            return baos.toByteArray();
        }
        else if (message instanceof ObjectMessage)
        {
            ObjectMessage oMsg = (ObjectMessage)message;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(oMsg.getObject());
            os.flush();
            os.close();
            return baos.toByteArray();
        }
        else if (message instanceof TextMessage)
        {
            TextMessage tMsg = (TextMessage)message;
            String tMsgText = tMsg.getText();

            if (null == tMsgText)
            {
                // Avoid creating new instances of byte arrays, even empty ones. The
                // load on this part of the code can be high.
                return ArrayUtils.EMPTY_BYTE_ARRAY;
            }
            else
            {
                return tMsgText.getBytes();
            }
        }
        else
        {
            throw new JMSException("Cannot get bytes from Map Message");
        }
    }

    public static String getNameForDestination(Destination dest) throws JMSException
    {
        if (dest instanceof Queue)
        {
            return ((Queue)dest).getQueueName();
        }
        else if (dest instanceof Topic)
        {
            return ((Topic)dest).getTopicName();
        }
        else
        {
            return null;
        }
    }

    public static Message copyJMSProperties(Message from, Message to, JmsConnector connector)
        throws JMSException
    {
        if (connector.supportsProperty(JmsConstants.JMS_CORRELATION_ID))
        {
            to.setJMSCorrelationID(from.getJMSCorrelationID());
        }
        if (connector.supportsProperty(JmsConstants.JMS_DELIVERY_MODE))
        {
            to.setJMSDeliveryMode(from.getJMSDeliveryMode());
        }
        if (connector.supportsProperty(JmsConstants.JMS_DESTINATION))
        {
            to.setJMSDestination(from.getJMSDestination());
        }
        if (connector.supportsProperty(JmsConstants.JMS_EXPIRATION))
        {
            to.setJMSExpiration(from.getJMSExpiration());
        }
        if (connector.supportsProperty(JmsConstants.JMS_MESSAGE_ID))
        {
            to.setJMSMessageID(from.getJMSMessageID());
        }
        if (connector.supportsProperty(JmsConstants.JMS_PRIORITY))
        {
            to.setJMSPriority(from.getJMSPriority());
        }
        if (connector.supportsProperty(JmsConstants.JMS_REDELIVERED))
        {
            to.setJMSRedelivered(from.getJMSRedelivered());
        }
        if (connector.supportsProperty(JmsConstants.JMS_REPLY_TO))
        {
            to.setJMSReplyTo(from.getJMSReplyTo());
        }
        if (connector.supportsProperty(JmsConstants.JMS_TIMESTAMP))
        {
            to.setJMSTimestamp(from.getJMSTimestamp());
        }
        if (connector.supportsProperty(JmsConstants.JMS_TYPE))
        {
            to.setJMSType(from.getJMSType());
        }
        return to;
    }
}
