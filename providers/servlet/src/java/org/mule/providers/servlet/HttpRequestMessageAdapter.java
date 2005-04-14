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
 */
package org.mule.providers.servlet;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.http.HttpConstants;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UniqueIdNotSupportedException;
import org.mule.util.IteratorAdapter;
import org.mule.util.Utility;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Iterator;

/**
 * <code>HttpRequestMessageAdapter</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class HttpRequestMessageAdapter implements UMOMessageAdapter
{
    public static final String PAYLOAD_PARAMETER_NAME = "org.mule.servlet.payload.param";
    public static final String DEFAULT_PAYLOAD_PARAMETER_NAME = "payload";

    private Object message = null;

    protected UMOExceptionPayload exceptionPayload;

    private HttpServletRequest request;

    public HttpRequestMessageAdapter(Object message) throws MessagingException
    {
        setPayload(message);
    }

    /* (non-Javadoc)
     * @see org.mule.umo.providers.UMOMessageAdapter#getMessage()
     */
    public Object getPayload()
    {
        return message;
    }

    public boolean isBinary() {
        return message instanceof byte[];
    }

    /* (non-Javadoc)
     * @see org.mule.umo.providers.UMOMessageAdapter#getMessageAsBytes()
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        if(isBinary()) {
            return (byte[])message;
        } else {
            return ((String)message).getBytes();
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.providers.UMOMessageAdapter#getMessageAsString()
     */
    public String getPayloadAsString() throws Exception
    {
        if(isBinary()) {
            return new String((byte[])message);
        } else {
            return (String)message;
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.providers.UMOMessageAdapter#setMessage(java.lang.Object)
     */
    private void setPayload(Object message) throws MessagingException
    {
        if (message instanceof HttpServletRequest)
        {
            try
            {
                request = (HttpServletRequest) message;
                String payloadParam = (String)request.getAttribute(PAYLOAD_PARAMETER_NAME);

                if(payloadParam==null) {
                    payloadParam = DEFAULT_PAYLOAD_PARAMETER_NAME;
                }
                String payload = request.getParameter(payloadParam);
                if(payload==null) {
                    if(isText(request.getContentType())) {
                        this.message = Utility.inputStreamToString(request.getInputStream(),4096);
                    } else {
                        this.message = Utility.inputStreamToByteArray(request.getInputStream(), 4096);
                    }
                } else {
                    this.message = payload;
                }
            } catch (IOException e)
            {
                throw new MessagingException(new Message("servlet", 3, request.getRequestURL().toString()), e);
            }

        }else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    public HttpServletRequest getRequest()
    {
        return request;
    }

    public Object getProperty(Object key)
    {
        if(key==null) return null;
        Object prop = getRequest().getHeader(key.toString());
        if(prop==null) {
            prop = getRequest().getParameter(key.toString());
            if(prop==null) {
                prop = getRequest().getAttribute(key.toString());
            }
        }
        return prop;
    }

    public void setProperty(Object key, Object value)
    {
        if(key==null) return;
        getRequest().setAttribute(key.toString(), value);
    }

    public Object removeProperty(Object key)
    {
        if(key==null) return null;
        Object att = getRequest().getAttribute(key.toString());
        if(att!=null) getRequest().removeAttribute(key.toString());
        return att;
    }

    public Iterator getPropertyNames()
    {
        return new IteratorAdapter(getRequest().getParameterNames());
    }

    public String getUniqueId() throws UniqueIdNotSupportedException
    {
        if(getRequest().getSession()==null) {
            throw new UniqueIdNotSupportedException(this, new Message(Messages.X_IS_NULL, "Http session"));
        }
        return getRequest().getSession().getId();
    }

    protected boolean isText(String contentType) {
        if(contentType==null) return true;
        return (contentType.startsWith("text/"));
    }

    public Object getProperty(String name, Object defaultValue)
    {
        Object result = getProperty(name);
        if(result==null) return defaultValue;
        return result;
    }

    public int getIntProperty(String name, int defaultValue)
    {
        Object result = getProperty(name);
        if(result!=null && result instanceof Integer)
        {
            return ((Integer)result).intValue();
        }
        return defaultValue;
    }

    public long getLongProperty(String name, long defaultValue)
    {
        Object result = getProperty(name);
        if(result!=null && result instanceof Long)
        {
            return ((Long)result).longValue();
        }
        return defaultValue;
    }

    public double getDoubleProperty(String name, double defaultValue)
    {
        Object result = getProperty(name);
        if(result!=null && result instanceof Double)
        {
            return ((Double)result).doubleValue();
        }
        return defaultValue;
    }

    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        Object result = getProperty(name);
        if(result!=null && result instanceof Boolean)
        {
            return ((Boolean)result).booleanValue();
        }
        return defaultValue;
    }

    public void setBooleanProperty(String name, boolean value)
    {
        getRequest().setAttribute(name, new Boolean(value));
    }

    public void setIntProperty(String name, int value)
    {
        getRequest().setAttribute(name, new Integer(value));
    }

    public void setLongProperty(String name, long value)
    {
        getRequest().setAttribute(name, new Long(value));
    }

    public void setDoubleProperty(String name, double value)
    {
        getRequest().setAttribute(name, new Double(value));
    }

    /**
     * Sets a correlationId for this message.  The correlation Id can
     * be used by components in the system to manage message relations
     * <p/>
     * transport protocol.  As such not all messages will support the notion
     * of a correlationId i.e. tcp or file.  In this situation the correlation Id
     * is set as a property of the message where it's up to developer to keep
     * the association with the message. For example if the message is serialised to
     * xml the correlationId will be available in the message.
     *
     * @param id the Id reference for this relationship
     */
    public void setCorrelationId(String id)
    {
        setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, id);
    }

    /**
     * Sets a correlationId for this message.  The correlation Id can
     * be used by components in the system to manage message relations.
     * <p/>
     * The correlationId is associated with the message using the underlying
     * transport protocol.  As such not all messages will support the notion
     * of a correlationId i.e. tcp or file.  In this situation the correlation Id
     * is set as a property of the message where it's up to developer to keep
     * the association with the message. For example if the message is serialised to
     * xml the correlationId will be available in the message.
     *
     * @return the correlationId for this message or null if one hasn't been set
     */
    public String getCorrelationId()
    {
        return (String)getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
    }

    /**
     * Sets a replyTo address for this message.  This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response needs
     * to be routed somewhere for further processing.
     * The value of this field can be any valid endpointUri url.
     *
     * @param replyTo the endpointUri url to reply to
     */
    public void setReplyTo(Object replyTo)
    {
        if(replyTo!=null && replyTo.toString().startsWith("http")) {
            setProperty(HttpConstants.HEADER_LOCATION, replyTo);
        }
        setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, replyTo);

    }

    /**
     * Sets a replyTo address for this message.  This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response needs
     * to be routed somewhere for further processing.
     * The value of this field can be any valid endpointUri url.
     *
     * @return the endpointUri url to reply to or null if one has not been set
     */
    public Object getReplyTo()
    {
        String replyto = (String)getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
        if(replyto==null) {
            replyto = (String)getProperty(HttpConstants.HEADER_LOCATION);
        }
        return replyto;
    }

    /**
     * Gets the sequence or ordering number for this message in the
     * the correlation group (as defined by the correlationId)
     *
     * @return the sequence number  or -1 if the sequence is not important
     */
    public int getCorrelationSequence()
    {
        return getIntProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, -1);
    }

    /**
     * Gets the sequence or ordering number for this message in the
     * the correlation group (as defined by the correlationId)
     *
     * @param sequence the sequence number  or -1 if the sequence is not important
     */
    public void setCorrelationSequence(int sequence)
    {
        setIntProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, sequence);
    }

    /**
     * Determines how many messages are in the correlation group
     *
     * @return total messages in this group or -1 if the size is not known
     */
    public int getCorrelationGroupSize()
    {
        return getIntProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, -1);
    }

    /**
     * Determines how many messages are in the correlation group
     *
     * @param size the total messages in this group or -1 if the size is not known
     */
    public void setCorrelationGroupSize(int size)
    {
        setIntProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, size);
    }

    public UMOExceptionPayload getExceptionPayload() {
        return exceptionPayload;
    }

    public void setExceptionPayload(UMOExceptionPayload exceptionPayload) {
        this.exceptionPayload = exceptionPayload;
    }
}
