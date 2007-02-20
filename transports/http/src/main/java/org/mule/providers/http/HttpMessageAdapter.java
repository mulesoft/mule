/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.NameValuePair;

/**
 * <code>HttpMessageAdapter</code> Wraps an incoming Http Request making the
 * payload and headers available as standard message adapter.
 */
public class HttpMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1544495479333000422L;

    private static final UMOTransformer transformer = new SerializableToByteArray();

    private final Object message;
    private boolean http11 = true;

    public HttpMessageAdapter(Object message) throws MessagingException
    {
        if (message instanceof Object[])
        {
            this.message = ((Object[])message)[0];
            if (((Object[])message).length > 1)
            {
                Map props = (Map)((Object[])message)[1];
                for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();)
                {
                    Map.Entry e = (Map.Entry)iterator.next();
                    String key = (String)e.getKey();
                    Object value = e.getValue();
                    // skip incoming null values
                    if (value != null)
                    {
                        setProperty(key, value);
                    }
                }
            }
        }
        else if (message instanceof byte[])
        {
            this.message = message;
            // If the adapter is being created as part of a response flow, just wrap
            // the HttpResponse
        }
        else if (message instanceof HttpResponse)
        {
            this.message = message;
            return;
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
        String temp = getStringProperty(HttpConnector.HTTP_VERSION_PROPERTY, null);
        if (HttpConstants.HTTP10.equalsIgnoreCase(temp))
        {
            http11 = false;
        }

        // set the encoding
        String charset = null;
        Header contenttype = getHeader(HttpConstants.HEADER_CONTENT_TYPE);
        if (contenttype != null)
        {
            HeaderElement values[] = contenttype.getElements();
            if (values.length == 1)
            {
                NameValuePair param = values[0].getParameterByName("charset");
                if (param != null)
                {
                    charset = param.getValue();
                }
            }
        }
        if (charset != null)
        {
            encoding = charset;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.providers.UMOMessageAdapter#getPayload()
     */
    public Object getPayload()
    {
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.providers.UMOMessageAdapter#getPayloadAsBytes()
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        if (message instanceof byte[])
        {
            return (byte[])message;
        }
        else if (message instanceof String)
        {
            return message.toString().getBytes();
        }
        else
        {
            return (byte[])transformer.transform(message);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.providers.UMOMessageAdapter#getPayloadAsString(String
     *      encoding)
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        if (message instanceof byte[])
        {
            if (encoding != null)
            {
                return new String((byte[])message, encoding);
            }
            else
            {
                return new String((byte[])message);
            }
        }
        else
        {
            return message.toString();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getProperty(java.lang.Object)
     */
    public Object getProperty(String key)
    {
        if (HttpConstants.HEADER_KEEP_ALIVE.equals(key) || HttpConstants.HEADER_CONNECTION.equals(key))
        {
            if (!http11)
            {
                String connection = super.getStringProperty(HttpConstants.HEADER_CONNECTION, null);
                if (connection != null && connection.equalsIgnoreCase("close"))
                {
                    return "false";
                }
                else
                {
                    return "true";
                }
            }
            else
            {
                return (super.getProperty(HttpConstants.HEADER_CONNECTION) != null ? "true" : "false");
            }
        }
        else
        {
            return super.getProperty(key);
        }
    }

    public Header getHeader(String name)
    {
        String value = getStringProperty(name, null);
        if (value == null)
        {
            return null;
        }
        return new Header(name, value);
    }
}
