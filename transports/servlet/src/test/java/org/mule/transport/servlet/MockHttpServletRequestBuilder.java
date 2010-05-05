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

import org.mule.transport.http.HttpConstants;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.Enumeration;
import java.util.HashMap;
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
    public Map<String, String> headers = new HashMap<String, String>();
    public int localPort = 8080;
    
    public HttpServletRequest buildRequest()
    {
        Mock mockRequest = new Mock(HttpServletRequest.class);

        mockRequest.expectAndReturn("getMethod", method);
        mockRequest.expectAndReturn("getRequestURI", requestUri);
        mockRequest.expectAndReturn("getQueryString", queryString);
        mockRequest.expectAndReturn("getInputStream", inputStream);
        mockRequest.expectAndReturn("getSession", C.ANY_ARGS, session);
        mockRequest.expectAndReturn("getCharacterEncoding", characterEncoding);
        mockRequest.expectAndReturn("getLocalPort", localPort);
        
        mockRequest.expectAndReturn("getContentType", contentType);
        mockRequest.expectAndReturn("getContentType", contentType);

        addParameterExpectations(mockRequest);
        addAttributeExpectations(mockRequest);
        addHeaderExpectations(mockRequest);
        
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
            addMapExpectations(mockRequest, "getAttribute", attributes);
        }
        
        mockRequest.expectAndReturn("getAttributeNames", nameEnum);
    }

    private void addHeaderExpectations(Mock mockRequest)
    {
        Enumeration<?> nameEnum = null;
        if (headers != null)
        {
            nameEnum = keyEnumeration(headers);
            addMapExpectations(mockRequest, "getHeader", headers);
        }
        
        mockRequest.expectAndReturn("getHeaderNames", nameEnum);
    }
    
    private void addMapExpectations(Mock mockRequest, String methodName, Map<String, String> map)
    {
        for (Map.Entry<String, String> entry : map.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue();
            mockRequest.expectAndReturn(methodName, C.eq(key), value);
        }
    }
    
    private Enumeration<?> keyEnumeration(Map<String, String> map)
    {
        Set<String> keys = map.keySet();
        return new IteratorEnumeration(keys.iterator());
    }
}
