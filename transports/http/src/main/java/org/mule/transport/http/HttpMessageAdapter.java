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

import java.util.HashMap;
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

    private boolean http11 = true;

    private Object message;

    public HttpMessageAdapter(Object message)
    {
        if (message instanceof Object[])
        {
            // This case comes from the HttpMessageReceiver...
            Map headers = new HashMap();
            this.message = ((Object[]) message)[0];
            if (((Object[]) message).length > 1)
            {
                Object second = ((Object[]) message)[1];
                if (second instanceof Map)
                {
                    Map props = (Map) second;
                    for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();)
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
                else if (second instanceof Header[])
                {
                    Header[] inboundHeaders = (Header[]) second;
                    for (int i = 0; i < inboundHeaders.length; i++)
                    {
                        headers.put(inboundHeaders[i].getName(), inboundHeaders[i].getValue());
                    }
                }
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

        String temp = getStringProperty(HttpConnector.HTTP_VERSION_PROPERTY, null);
        if (HttpConstants.HTTP10.equalsIgnoreCase(temp))
        {
            http11 = false;
        }

        // set the encoding
        Header contenttype = getHeader(HttpConstants.HEADER_CONTENT_TYPE);
        if (contenttype != null)
        {
            HeaderElement values[] = contenttype.getElements();
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

    public ThreadSafeAccess newThreadCopy()
    {
        return new HttpMessageAdapter(this);
    }

}
