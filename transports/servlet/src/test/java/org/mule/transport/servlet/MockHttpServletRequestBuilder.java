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

import org.mule.transport.http.HttpConstants;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

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
    
    public HttpServletRequest buildRequest()
    {
        Mock mockRequest = new Mock(HttpServletRequest.class);

        mockRequest.expectAndReturn("getMethod", method);
        
        Enumeration<?> emptyEnumeration = new Hashtable<Object, Object>().elements();
        mockRequest.expectAndReturn("getParameterNames", emptyEnumeration);
        
        mockRequest.expectAndReturn("getRequestURI", requestUri);
        mockRequest.expectAndReturn("getQueryString", queryString);
        mockRequest.expectAndReturn("getInputStream", inputStream);
        mockRequest.expectAndReturn("getSession", C.ANY_ARGS, session);
        mockRequest.expectAndReturn("getCharacterEncoding", characterEncoding);
        mockRequest.expectAndReturn("getLocalPort", localPort);
        
        mockRequest.expectAndReturn("getContentType", contentType);
        mockRequest.expectAndReturn("getContentType", contentType);

        mockRequest.expectAndReturn("getRemoteAddr", host);
        
        addParameterExpectations(mockRequest);
        addAttributeExpectations(mockRequest);
        addHeaderExpectations(mockRequest);
        mockRequest.expectAndReturn("getHeader", C.eq(HttpConstants.HEADER_HOST), host);
        
        return (HttpServletRequest) mockRequest.proxy();
    }

    private void addParameterExpectations(Mock mockRequest)
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
                mockRequest.expectAndReturn("getParameterValues", C.eq(key), value);
            }
        }

        mockRequest.expectAndReturn("getParameterNames", nameEnum);
        mockRequest.expectAndReturn("getParameterMap", parameters);
    }
    
    private void addAttributeExpectations(Mock mockRequest)
    {
        Enumeration<?> nameEnum = null;
        
        if (attributes != null)
        {
            nameEnum = keyEnumeration(attributes);
            
            for (Map.Entry<String, String> entry : attributes.entrySet())
            {
                String key = entry.getKey();
                String value = entry.getValue();
                
                mockRequest.expectAndReturn("getAttribute", C.eq(key), value);
            }
        }
        
        mockRequest.expectAndReturn("getAttributeNames", nameEnum);
    }

    private void addHeaderExpectations(Mock mockRequest)
    {
        Enumeration<?> nameEnum = null;
        if (headers != null)
        {
            nameEnum = keyEnumeration(headers);
            
            for (Map.Entry<String, Object> entry : headers.entrySet())
            {
                String key = entry.getKey();
                Object value = entry.getValue();
                if ((value instanceof Enumeration<?>) == false)
                {
                    value = new SingleElementEnumeration(value);
                }
                
                mockRequest.expectAndReturn("getHeaders", C.eq(key), value);
            }
        }
        
        mockRequest.expectAndReturn("getHeaderNames", nameEnum);
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
