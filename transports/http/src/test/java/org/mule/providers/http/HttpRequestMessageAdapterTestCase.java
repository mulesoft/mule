/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.providers.http.servlet.HttpRequestMessageAdapter;
import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

public class HttpRequestMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    public Object getValidMessage() throws Exception
    {
        return getMockRequest("test message");
    }

    public UMOMessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new HttpRequestMessageAdapter(payload);
    }

    public static HttpServletRequest getMockRequest(final String message)
    {
        Object proxy = Proxy.newProxyInstance(ServletConnectorTestCase.class.getClassLoader(),
            new Class[]{HttpServletRequest.class}, new InvocationHandler()
            {
                private String payload = message;
                private Map props = new HashMap();

                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
                {
                    if ("getInputStream".equals(method.getName()))
                    {

                        ServletInputStream s = new ServletInputStream()
                        {
                            ByteArrayInputStream is = new ByteArrayInputStream(payload.getBytes());

                            public int read() throws IOException
                            {
                                return is.read();
                            }
                        };
                        return s;

                    }
                    else if ("getAttribute".equals(method.getName()))
                    {
                        return props.get(args[0]);
                    }
                    else if ("setAttribute".equals(method.getName()))
                    {
                        props.put(args[0], args[1]);
                    }
                    else if ("equals".equals(method.getName()))
                    {
                        return Boolean.valueOf(payload.equals(args[0].toString()));
                    }
                    else if ("toString".equals(method.getName()))
                    {
                        return payload;
                    }
                    else if ("getReader".equals(method.getName()))
                    {
                        return new BufferedReader(new StringReader(payload.toString()));
                    }
                    else if ("getAttributeNames".equals(method.getName()))
                    {
                        return new Hashtable().elements();
                    }
                    else if ("getHeaderNames".equals(method.getName()))
                    {
                        return new Hashtable().elements();
                    }
                    return null;
                }
            });
        return (HttpServletRequest)proxy;
    }
}
