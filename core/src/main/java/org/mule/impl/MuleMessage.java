/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.MuleRuntimeException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.DefaultMessageAdapter;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.UMOMessageAdapter;

import javax.activation.DataHandler;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <code>MuleMessage</code> is a wrapper that contains a payload and properties
 * associated with the payload.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MuleMessage implements UMOMessage
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1541720810851984842L;

    private UMOMessageAdapter adapter;

    protected UMOExceptionPayload exceptionPayload;

    public MuleMessage(Object message)
    {
        this(message, (Map)null);
    }

    public MuleMessage(Object message, Map properties)
    {
        if (message instanceof UMOMessageAdapter)
        {
            adapter = (UMOMessageAdapter)message;
        }
        else
        {
            adapter = new DefaultMessageAdapter(message);
        }
        addProperties(properties);
    }

    public MuleMessage(Object message, UMOMessageAdapter previous)
    {
        if (message instanceof UMOMessageAdapter)
        {
            adapter = (UMOMessageAdapter)message;
        }
        else
        {
            adapter = new DefaultMessageAdapter(message, previous);
        }
        if (previous.getExceptionPayload() != null)
        {
            setExceptionPayload(previous.getExceptionPayload());
        }
        setEncoding(previous.getEncoding());
        if (previous.getAttachmentNames().size() > 0)
        {
            Set attNames = adapter.getAttachmentNames();
            synchronized (attNames)
            {
                for (Iterator iterator = attNames.iterator(); iterator.hasNext();)
                {
                    String s = (String)iterator.next();
                    try
                    {
                        addAttachment(s, adapter.getAttachment(s));
                    }
                    catch (Exception e)
                    {
                        throw new MuleRuntimeException(new Message(Messages.FAILED_TO_READ_ATTACHMENT_X, s),
                            e);
                    }
                }
            }
        }
    }

    public UMOMessageAdapter getAdapter()
    {
        return adapter;
    }

    /**
     * Gets a property of the payload implementation
     * 
     * @param key the key on which to lookup the property value
     * @return the property value or null if the property does not exist
     */
    public Object getProperty(String key)
    {
        return adapter.getProperty(key);
    }

    public Object removeProperty(String key)
    {
        return adapter.removeProperty(key);
    }

    /**
     * Gets a property of the payload implementation
     * 
     * @param key the key on which to associate the value
     * @param value the property value
     */
    public void setProperty(String key, Object value)
    {
        adapter.setProperty(key, value);
    }

    /**
     * Converts the payload implementation into a String representation
     * 
     * @return String representation of the payload
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public String getPayloadAsString() throws Exception
    {
        return adapter.getPayloadAsString();
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @param encoding The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        if (encoding == null)
        {
            return adapter.getPayloadAsString();
        }
        else
        {
            return adapter.getPayloadAsString(encoding);
        }
    }

    /**
     * @return all properties on this payload
     */
    public Set getPropertyNames()
    {
        return adapter.getPropertyNames();
    }

    /**
     * Converts the payload implementation into a String representation
     * 
     * @return String representation of the payload
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return adapter.getPayloadAsBytes();
    }

    /**
     * @return the current payload
     */
    public Object getPayload()
    {
        return adapter.getPayload();
    }

    public void addProperties(Map properties)
    {
        adapter.addProperties(properties);
    }

    public void clearProperties()
    {
        adapter.clearProperties();
    }

    /**
     * Gets a double property from the event
     * 
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     */
    public double getDoubleProperty(String name, double defaultValue)
    {
        return adapter.getDoubleProperty(name, defaultValue);
    }

    /**
     * Sets a double property on the event
     * 
     * @param name the property name or key
     * @param value the property value
     */
    public void setDoubleProperty(String name, double value)
    {
        adapter.setDoubleProperty(name, value);
    }

    public String getUniqueId()
    {
        return adapter.getUniqueId();
    }

    public Object getProperty(String name, Object defaultValue)
    {
        return adapter.getProperty(name, defaultValue);
    }

    public int getIntProperty(String name, int defaultValue)
    {
        return adapter.getIntProperty(name, defaultValue);
    }

    public long getLongProperty(String name, long defaultValue)
    {
        return adapter.getLongProperty(name, defaultValue);
    }

    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        return adapter.getBooleanProperty(name, defaultValue);
    }

    public void setBooleanProperty(String name, boolean value)
    {
        adapter.setBooleanProperty(name, value);
    }

    public void setIntProperty(String name, int value)
    {
        adapter.setIntProperty(name, value);
    }

    public void setLongProperty(String name, long value)
    {
        adapter.setLongProperty(name, value);
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
        adapter.setCorrelationId(id);
    }

    /**
     * Sets a correlationId for this message. The correlation Id can be used by
     * components in the system to manage message relations. <p/> The correlationId
     * is associated with the message using the underlying transport protocol. As
     * such not all messages will support the notion of a correlationId i.e. tcp or
     * file. In this situation the correlation Id is set as a property of the message
     * where it's up to developer to keep the association with the message. For
     * example if the message is serialised to xml the correlationId will be
     * available in the message.
     * 
     * @return the correlationId for this message or null if one hasn't been set
     */
    public String getCorrelationId()
    {
        return adapter.getCorrelationId();
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
        adapter.setReplyTo(replyTo);
    }

    /**
     * Sets a replyTo address for this message. This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response
     * needs to be routed somewhere for further processing. The value of this field
     * can be any valid endpointUri url.
     * 
     * @return the endpointUri url to reply to or null if one has not been set
     */
    public Object getReplyTo()
    {
        return adapter.getReplyTo();
    }

    /**
     * Gets the sequence or ordering number for this message in the the correlation
     * group (as defined by the correlationId)
     * 
     * @return the sequence number or -1 if the sequence is not important
     */
    public int getCorrelationSequence()
    {
        return adapter.getCorrelationSequence();
    }

    /**
     * Gets the sequence or ordering number for this message in the the correlation
     * group (as defined by the correlationId)
     * 
     * @param sequence the sequence number or -1 if the sequence is not important
     */
    public void setCorrelationSequence(int sequence)
    {
        adapter.setCorrelationSequence(sequence);
    }

    /**
     * Determines how many messages are in the correlation group
     * 
     * @return total messages in this group or -1 if the size is not known
     */
    public int getCorrelationGroupSize()
    {
        return adapter.getCorrelationGroupSize();
    }

    /**
     * Determines how many messages are in the correlation group
     * 
     * @param size the total messages in this group or -1 if the size is not known
     */
    public void setCorrelationGroupSize(int size)
    {
        adapter.setCorrelationGroupSize(size);
    }

    public UMOExceptionPayload getExceptionPayload()
    {
        return exceptionPayload;
    }

    public void setExceptionPayload(UMOExceptionPayload exceptionPayload)
    {
        this.exceptionPayload = exceptionPayload;
    }

    public String toString()
    {
        return adapter.toString();
    }

    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        adapter.addAttachment(name, dataHandler);
    }

    public void removeAttachment(String name) throws Exception
    {
        adapter.removeAttachment(name);
    }

    public DataHandler getAttachment(String name)
    {
        return adapter.getAttachment(name);
    }

    public Set getAttachmentNames()
    {
        return adapter.getAttachmentNames();
    }

    /**
     * Gets the encoding for the current message. For potocols that send encoding
     * Information with the message, this method should be overriden to expose the
     * transport encoding, otherwise the default encoding in the Mule configuration
     * will be used
     * 
     * @return the encoding for this message. This method must never return null
     */
    public String getEncoding()
    {
        return adapter.getEncoding();
    }

    /**
     * Sets the encoding for this message
     * 
     * @param encoding the encoding to use
     */
    public void setEncoding(String encoding)
    {
        adapter.setEncoding(encoding);
    }

    /**
     * Gets a String property from the event
     * 
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     */
    public String getStringProperty(String name, String defaultValue)
    {
        return adapter.getStringProperty(name, defaultValue);
    }

    /**
     * Sets a String property on the event
     * 
     * @param name the property name or key
     * @param value the property value
     */
    public void setStringProperty(String name, String value)
    {
        adapter.setStringProperty(name, value);
    }
}
