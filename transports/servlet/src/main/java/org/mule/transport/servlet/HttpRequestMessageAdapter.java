/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet;

import org.mule.api.MessagingException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.transport.AbstractMessageAdapter;
import org.mule.transport.http.HttpConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;

/**
 * <code>HttpRequestMessageAdapter</code> is a Mule message adapter for
 * {@link HttpServletRequest} objects.
 */
public class HttpRequestMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4238448252206941125L;

    private PayloadHolder payloadHolder;
        
    private String contentType;

    private String characterEncoding;
    
    public HttpRequestMessageAdapter(Object message) throws MessagingException
    {
        if (message instanceof HttpServletRequest)
        {
            HttpServletRequest request = (HttpServletRequest) message;
            
            setPayload(request);
            setContentEncoding((HttpServletRequest) message);
            setupUniqueId(request);

            contentType = request.getContentType();
            characterEncoding = request.getCharacterEncoding();
            
            Map<Object, Object> headers = new HashMap<Object, Object>();
            
            final Map parameterMap = request.getParameterMap();
            if (parameterMap != null && parameterMap.size() > 0)
            {
                for (Iterator iterator = parameterMap.entrySet().iterator(); iterator.hasNext();)
                {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    String key = (String) entry.getKey();
                    Object value = entry.getValue();
                    if (value != null)
                    {
                        if (value.getClass().isArray() && ((Object[]) value).length == 1)
                        {
                            headers.put(key, ((Object[]) value)[0]);
                        }
                        else
                        {
                            headers.put(key, value);
                        }
                    }
                }
            }
            String key;
            for (Enumeration e = request.getAttributeNames(); e.hasMoreElements();)
            {
                key = (String) e.nextElement();
                headers.put(key, request.getAttribute(key));
            }
            String realKey;
            for (Enumeration e = request.getHeaderNames(); e.hasMoreElements();)
            {
                key = (String)e.nextElement();
                realKey = key;
                if (key.startsWith(HttpConstants.X_PROPERTY_PREFIX))
                {
                    realKey = key.substring(2);
                }

                // Workaround for containers that strip the port from the Host header.
                // This is needed so Mule components can figure out what port they're on.
                String value = request.getHeader(key);
                if (HttpConstants.HEADER_HOST.equalsIgnoreCase(key)) 
                {
                    realKey = HttpConstants.HEADER_HOST;
                    int port = request.getLocalPort();
                    if (!value.contains(":") && port != 80 && port != 443)
                    {
                        value = value + ":" + port;
                    }
                }
                
                headers.put(realKey, value);
            }
            
            addInboundProperties(headers);
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    protected HttpRequestMessageAdapter(HttpRequestMessageAdapter template)
    {
        super(template);
        payloadHolder = template.payloadHolder;
        contentType = template.contentType;
        characterEncoding = template.characterEncoding;
    }

    protected void setContentEncoding(HttpServletRequest request)
    {
        String contentType = request.getContentType();
        if (contentType != null)
        {
            int i = contentType.indexOf("charset");
            if (i > -1)
            {
                int x = contentType.lastIndexOf(";");
                if (x > i)
                {
                    setEncoding(contentType.substring(i + 8, x));
                }
                else
                {
                    setEncoding(contentType.substring(i + 8));
                }
            }
        }
    }

    private void setPayload(HttpServletRequest request) throws MessagingException
    {
        if ("GET".equalsIgnoreCase(request.getMethod())) 
        {
            payloadHolder = new GetPayloadHolder(request);
        }
        else 
        {
            payloadHolder = new PostPayloadHolder(request);
        }
    }

    public Object getPayload()
    {
        return payloadHolder.getPayload();
    }
    
    private void setupUniqueId(HttpServletRequest request)
    {
        HttpSession session = null;

        try
        {
            // We wrap this call as on some App Servers (Websfear) it can cause an NPE
            session = request.getSession(false);
            id = session.getId();
        }
        catch (Exception e)
        {
            // super's default is good enough in this case
        }
    }

    /**
     * Sets a replyTo address for this message. This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response
     * needs to be routed somewhere for further processing. The value of this field
     * can be any valid endpointUri url.
     * 
     * @param replyTo the endpointUri url to reply to
     */
    @Override
    public void setReplyTo(Object replyTo)
    {
        if (replyTo != null && replyTo.toString().startsWith("http"))
        {
            setProperty(HttpConstants.HEADER_LOCATION, replyTo);
        }
        setProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, replyTo);
    }

    /**
     * Sets a replyTo address for this message. This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response
     * needs to be routed somewhere for further processing. The value of this field
     * can be any valid endpointUri url.
     * 
     * @return the endpointUri url to reply to or null if one has not been set
     */
    @Override
    public Object getReplyTo()
    {
        Object replyto = getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
        if (replyto == null)
        {
            replyto = getProperty(HttpConstants.HEADER_LOCATION);
        }
        return replyto;
    }

    public String getContentType()
    {
        return contentType;
    }
    
    public String getCharacterEncoding()
    {
        return characterEncoding;
    }

    public Enumeration<String> getParameterNames()
    {
        throw new UnsupportedOperationException("getParameterNames");
    }

    public String[] getParameterValues(String paramName)
    {
        throw new UnsupportedOperationException("getParameterValues");
    }

    @Override
    public ThreadSafeAccess newThreadCopy()
    {
        return new HttpRequestMessageAdapter(this);
    }

    private interface PayloadHolder extends Serializable
    {
        Object getPayload();
    }
    
    private static class GetPayloadHolder implements PayloadHolder
    {
        private String payload;
        
        public GetPayloadHolder(HttpServletRequest request)
        {
            super();
            payload = request.getRequestURI().toString() + "?" + request.getQueryString();
        }
        
        public Object getPayload()
        {
            return payload;
        }
    }
    
    private static class PostPayloadHolder implements PayloadHolder
    {
        private transient InputStream payload;

        public PostPayloadHolder(HttpServletRequest request)
        {
            super();
            try
            {
                payload = request.getInputStream();
            }
            catch (IOException iox)
            {
                throw new RuntimeException(iox);
            }
        }

        public Object getPayload()
        {
            return payload;
        }
        
        private void writeObject(ObjectOutputStream out) throws IOException
        {
            byte[] payloadBytes = IOUtils.toByteArray(payload);
            out.writeInt(payloadBytes.length);
            out.write(payloadBytes);
        }
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
        {
            int payloadSize = in.readInt();
            byte[] payloadBytes = new byte[payloadSize];
            in.read(payloadBytes);
            payload = new ByteArrayInputStream(payloadBytes);
        }
    }
}
