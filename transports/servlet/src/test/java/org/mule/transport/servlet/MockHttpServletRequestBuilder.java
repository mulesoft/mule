/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.transport.http.HttpConstants;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.iterators.IteratorEnumeration;

public class MockHttpServletRequestBuilder
{
    public static final String REQUEST_URI = "/services/Echo";

    public String method = HttpConstants.METHOD_GET;
    public String requestUri = REQUEST_URI;
    public ServletInputStream inputStream = null;
    public String payload = null;
    public String queryString = null;
    public Map<String, String[]> parameters = null;
    public String contentType = null;
    public HttpSession session = null;
    public String characterEncoding = null;
    public Map<String, String> attributes = new HashMap<String, String>();
    public Map<String, Object> headers = new HashMap<String, Object>();
    public String host = "localhost";
    public int localPort = 8080;
    public String pathInfo;

    public HttpServletRequest buildRequest() throws Exception
    {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getMethod()).thenReturn(method);

        Enumeration<?> emptyEnumeration = new Hashtable<Object, Object>().elements();
        when(mockRequest.getParameterNames()).thenReturn(emptyEnumeration);

        when(mockRequest.getRequestURI()).thenReturn(requestUri);
        when(mockRequest.getQueryString()).thenReturn(queryString);
        when(mockRequest.getInputStream()).thenReturn(inputStream);
        when(mockRequest.getSession(anyBoolean())).thenReturn(session);
        when(mockRequest.getCharacterEncoding()).thenReturn(characterEncoding);
        when(mockRequest.getLocalPort()).thenReturn(localPort);
        when(mockRequest.getContentType()).thenReturn(contentType);
        when(mockRequest.getRemoteAddr()).thenReturn(host);
        when(mockRequest.getHeader(eq(HttpConstants.HEADER_HOST))).thenReturn(host);
        when(mockRequest.getPathInfo()).thenReturn(pathInfo);

        addParameterExpectations(mockRequest);
        addAttributeExpectations(mockRequest);
        addHeaderExpectations(mockRequest);

        return mockRequest;
    }

    private void addParameterExpectations(HttpServletRequest mockRequest)
    {
        Enumeration<?> nameEnum = null;

        if (parameters != null)
        {
            Set<String> keys = parameters.keySet();
            nameEnum = new IteratorEnumeration(keys.iterator());

            for (Map.Entry<String, String[]> entry : parameters.entrySet())
            {
                String key = entry.getKey();
                String[] value = entry.getValue();
                when(mockRequest.getParameterValues(eq(key))).thenReturn(value);
                when(mockRequest.getParameter(eq(key))).thenReturn((value.length > 0) ?value[0] : null);
            }
        }

        when(mockRequest.getParameterNames()).thenReturn(nameEnum);
        when(mockRequest.getParameterMap()).thenReturn(parameters);
    }

    private void addAttributeExpectations(HttpServletRequest mockRequest)
    {
        Enumeration<?> nameEnum = null;

        if (attributes != null)
        {
            nameEnum = keyEnumeration(attributes);

            for (Map.Entry<String, String> entry : attributes.entrySet())
            {
                String key = entry.getKey();
                String value = entry.getValue();

                when(mockRequest.getAttribute(eq(key))).thenReturn(value);
            }
        }

        when(mockRequest.getAttributeNames()).thenReturn(nameEnum);
    }

    private void addHeaderExpectations(HttpServletRequest mockRequest)
    {
        Enumeration<?> nameEnum = null;
        if (headers != null)
        {
            nameEnum = keyEnumeration(headers);

            for (Map.Entry<String, Object> entry : headers.entrySet())
            {
                String key = entry.getKey();
                Object value = entry.getValue();

                Enumeration<?> valueAsEnumeration = null;
                if ((value instanceof Enumeration<?>) == false)
                {
                    valueAsEnumeration = new SingleElementEnumeration(value);
                }
                else
                {
                    valueAsEnumeration = (Enumeration<?>) value;
                }

                when(mockRequest.getHeaders(eq(key))).thenReturn(valueAsEnumeration);
            }
        }

        when(mockRequest.getHeaderNames()).thenReturn(nameEnum);
    }

    private Enumeration<?> keyEnumeration(Map<?, ?> map)
    {
        Set<?> keys = map.keySet();
        return new IteratorEnumeration(keys.iterator());
    }

    private static class SingleElementEnumeration implements Enumeration<Object>
    {
        private Object element;

        public SingleElementEnumeration(Object singleElement)
        {
            super();
            element = singleElement;
        }

        public boolean hasMoreElements()
        {
            return (element != null);
        }

        public Object nextElement()
        {
            Object retValue = element;
            if (element != null)
            {
                element = null;
            }
            return retValue;
        }
    }
}
