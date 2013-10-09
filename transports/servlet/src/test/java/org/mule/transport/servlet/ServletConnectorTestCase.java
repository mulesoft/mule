/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;

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

public class ServletConnectorTestCase extends AbstractConnectorTestCase
{
    @Override
    public Connector createConnector() throws Exception
    {
        ServletConnector c = new ServletConnector(muleContext);
        c.setName("test");
        return c;
    }

    @Override
    public String getTestEndpointURI()
    {
        return "servlet://testServlet";
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return getMockRequest("test message");
    }
    
    private HttpServletRequest getMockRequest(final String message)
    {
        Object proxy = Proxy.newProxyInstance(ServletConnectorTestCase.class.getClassLoader(),
            new Class[]{ HttpServletRequest.class }, new InvocationHandler()
            {
                private String payload = message;
                private Map<Object, Object> props = new HashMap<Object, Object>();

                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
                {
                    if ("getInputStream".equals(method.getName()))
                    {
                        ServletInputStream s = new ServletInputStream()
                        {
                            ByteArrayInputStream is = new ByteArrayInputStream(payload.getBytes());
    
                            @Override
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
                        return new Hashtable<Object, Object>().elements();
                    }
                    else if ("getHeaderNames".equals(method.getName()))
                    {
                        return new Hashtable<Object, Object>().elements();
                    }
                    return null;
                }
            }
        );
        return (HttpServletRequest) proxy;
    }

    @Override
    public void testConnectorMessageDispatcherFactory() throws Exception
    {
        // there is no DispatcherFactory for the servlet connector
    }

    public void testConnectorMessageDispatcher() throws Exception
    {
        // therefore we have no dispatchers
    }
}
