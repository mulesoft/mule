/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.api.ThreadSafeAccess;
import org.mule.transport.AbstractMessageAdapter;
import org.mule.transport.MessageAdapterSerialization;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.lang.SerializationUtils;

/**
 * <code>HttpMessageAdapter</code> Wraps an incoming Http Request making the
 * payload and headers available as standard message adapter.
 */
public class HttpMessageAdapter extends AbstractMessageAdapter implements MessageAdapterSerialization
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1544495479333000422L;

    private boolean http11 = true;

    private Object message;

    public HttpMessageAdapter(Object message)
    {
        if (message instanceof Object[])
        {
            // This case comes from the HttpMessageReceiver...
            Object[] messageParts = (Object[]) message;
            
            this.message = messageParts[0];

            Map<Object, Object> headers = new HashMap<Object, Object>();
            if (messageParts.length > 1)
            {
                Object second = messageParts[1];
                if (second instanceof Map)
                {
                    setupHeadersFromMap(headers, (Map) second);
                }
                else if (second instanceof Header[])
                {
                    setupHeadersFromHeaderArray(headers, (Header[]) second);
                }
                
                determineHttpVersion(headers);
                rewriteConnectionAndKeepAliveHeaders(headers);
                
                addInboundProperties(headers);
            }
        }
        else if (message instanceof HttpResponse)
        {
            this.message = message;
            return;
        }
        else
        {
            this.message = message;
        }

        String contentType = getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, null);
        if (contentType != null)
        {
            // set the encoding
            Header contentTypeHeader = new Header(HttpConstants.HEADER_CONTENT_TYPE, contentType);
            HeaderElement values[] = contentTypeHeader.getElements();
            if (values.length == 1)
            {
                NameValuePair param = values[0].getParameterByName("charset");
                if (param != null)
                {
                    setEncoding(param.getValue());
                }
            }
        }
    }

    private void setupHeadersFromMap(Map<Object, Object> headers, Map props)
    {
        for (Iterator<?> iterator = props.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry e = (Map.Entry) iterator.next();
            String key = (String) e.getKey();
            Object value = e.getValue();
            // skip incoming null values
            if (value != null)
            {
                headers.put(key, value);
            }
        }        
    }

    private void setupHeadersFromHeaderArray(Map<Object, Object> headers, Header[] inboundHeaders)
    {
        for (int i = 0; i < inboundHeaders.length; i++)
        {
            headers.put(inboundHeaders[i].getName(), inboundHeaders[i].getValue());
        }        
    }

    private void determineHttpVersion(Map headers)
    {
        String httpVersion = (String) headers.get(HttpConnector.HTTP_VERSION_PROPERTY);
        if (HttpConstants.HTTP10.equalsIgnoreCase(httpVersion))
        {
            http11 = false;
        }        
    }

    private void rewriteConnectionAndKeepAliveHeaders(Map<Object, Object> headers)
    {
        // rewrite Connection and Keep-Alive headers based on HTTP version
        String headerValue = null;
        if (!http11)
        {
            String connection = (String) headers.get(HttpConstants.HEADER_CONNECTION);
            if ((connection != null) && connection.equalsIgnoreCase("close"))
            {
                headerValue = "false";
            }
            else
            {
                headerValue = "true";
            }
        }
        else
        {
            headerValue =  (headers.get(HttpConstants.HEADER_CONNECTION) != null ? "true" : "false");
        }

        headers.put(HttpConstants.HEADER_CONNECTION, headerValue);
        headers.put(HttpConstants.HEADER_KEEP_ALIVE, headerValue);        
    }

    protected HttpMessageAdapter(HttpMessageAdapter template)
    {
        super(template);
        message = template.message;
        http11 = template.http11;
    }

    /** @return the current message */
    public Object getPayload()
    {
        return message;
    }

    /**
     * @deprecated use getStringProperty
     */
    @Deprecated
    public Header getHeader(String name)
    {
        String value = getStringProperty(name, null);
        if (value == null)
        {
            return null;
        }
        return new Header(name, value);
    }

    @Override
    public ThreadSafeAccess newThreadCopy()
    {
        return new HttpMessageAdapter(this);
    }

    public byte[] getPayloadForSerialization() throws Exception
    {        
        if (message instanceof InputStream)
        {
            // message is an InputStream when the HTTP method was POST
            return IOUtils.toByteArray((InputStream) message);
        }
        else if (message instanceof Serializable)
        {
            // message is a String when the HTTP method was GET
            return SerializationUtils.serialize((Serializable) message);
        }
        else
        {
            throw new NotSerializableException("Don't know how to serialize payload of type " 
                + message.getClass().getName());
        }
    }
    
}
