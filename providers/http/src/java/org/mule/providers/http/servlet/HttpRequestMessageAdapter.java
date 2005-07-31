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
package org.mule.providers.http.servlet;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.http.HttpConstants;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.provider.UniqueIdNotSupportedException;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

/**
 * <code>HttpRequestMessageAdapter</code> is a MUle message adapter
 * for javax.servletHttpServletRequest objects
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class HttpRequestMessageAdapter extends AbstractMessageAdapter
{
    private Object message = null;

    private HttpServletRequest request;

    public HttpRequestMessageAdapter(Object message) throws MessagingException
    {
        if(message instanceof HttpServletRequest) {
            setPayload((HttpServletRequest)message);
            if(request.getParameterMap()!=null) {
                properties.putAll(request.getParameterMap());
            }
            String key;
            for(Enumeration e = request.getAttributeNames();e.hasMoreElements();) {
                key = (String)e.nextElement();
                properties.put(key, request.getAttribute(key));
            }
            for(Enumeration e = request.getHeaderNames();e.hasMoreElements();) {
                key = (String)e.nextElement();
                properties.put(key, request.getHeader(key));
            }
        } else {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.providers.UMOMessageAdapter#getMessage()
     */
    public Object getPayload()
    {
        return message;
    }

    public boolean isBinary()
    {
        return message instanceof byte[];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.providers.UMOMessageAdapter#getMessageAsBytes()
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        if (isBinary()) {
            return (byte[]) message;
        } else {
            return ((String) message).getBytes();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.providers.UMOMessageAdapter#getMessageAsString()
     */
    public String getPayloadAsString() throws Exception
    {
        if (isBinary()) {
            return new String((byte[]) message);
        } else {
            return (String) message;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.providers.UMOMessageAdapter#setMessage(java.lang.Object)
     */
    private void setPayload(HttpServletRequest message) throws MessagingException
    {
        try {
            request =  message;
            //Check if a payload parameter has been set, if so use it
            //otherwise we'll use the request payload
            String payloadParam = (String) request.getAttribute(AbstractReceiverServlet.PAYLOAD_PARAMETER_NAME);

            if (payloadParam == null) {
                payloadParam = AbstractReceiverServlet.DEFAULT_PAYLOAD_PARAMETER_NAME;
            }
            String payload = request.getParameter(payloadParam);
            if (payload == null) {
                if (isText(request.getContentType())) {
                    BufferedReader reader = request.getReader();
                    StringBuffer buffer = new StringBuffer();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                        buffer.append("\n");
                    }
                    this.message = buffer.toString();
                } else {
                    InputStream is = request.getInputStream();
                    ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[1024*32];
                    int len = 0;
                    while ((len = is.read(buffer, len, buffer.length)) != -1) {
                        baos.write(buffer, 0, len);
                        if (len != buffer.length) {
                            break;
                        }
                    }
                    baos.flush();
                    this.message = baos.toByteArray();
                    baos.close();
                }
            } else {
                this.message = payload;
            }

        } catch (IOException e) {
            throw new MessagingException(new Message("servlet", 3, request.getRequestURL().toString()), e);
        }
    }

    public HttpServletRequest getRequest()
    {
        return request;
    }


    public String getUniqueId() throws UniqueIdNotSupportedException
    {
        if (getRequest().getSession() == null) {
            throw new UniqueIdNotSupportedException(this, new Message(Messages.X_IS_NULL, "Http session"));
        }
        return getRequest().getSession().getId();
    }

    protected boolean isText(String contentType)
    {
        if (contentType == null)
            return true;
        return (contentType.startsWith("text/"));
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
        if (replyTo != null && replyTo.toString().startsWith("http")) {
            setProperty(HttpConstants.HEADER_LOCATION, replyTo);
        }
        setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, replyTo);

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
        String replyto = (String) getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
        if (replyto == null) {
            replyto = (String) getProperty(HttpConstants.HEADER_LOCATION);
        }
        return replyto;
    }
}
