/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.servlet;

import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.commons.collections.iterators.IteratorEnumeration;

/**
 * THIS CLASS IS UNSUPPORTED AND THE IMPLEMENTATION DOES NOT CONFORM TO THE SERVLET SPECIFICATION!
 */
public class MuleHttpServletRequest implements HttpServletRequest
{
    private MuleEvent event;
    private MuleMessage message;

    public MuleHttpServletRequest(MuleEvent event)
    {
        super();
        this.event = event;
        this.message = event.getMessage();
    }

    @Override
    public Object getAttribute(String name)
    {
        return null;
    }

    @Override
    public Enumeration getAttributeNames()
    {
        return null;
    }

    @Override
    public String getCharacterEncoding()
    {
        return event.getEncoding();
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException
    {
        message.setEncoding(env);
    }

    @Override
    public int getContentLength()
    {
        return -1;
    }

    @Override
    public String getContentType()
    {
        return message.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        return new ServletInputStream()
        {

            @Override
            public int read() throws IOException
            {
                return 0;
            }
        };
    }

    @Override
    public String getParameter(String name)
    {
        return null;
    }

    @Override
    public Enumeration getParameterNames()
    {
        return null;
    }

    @Override
    public String[] getParameterValues(String name)
    {
        return null;
    }

    @Override
    public Map getParameterMap()
    {
        return null;
    }

    @Override
    public String getProtocol()
    {
        return null;
    }

    @Override
    public String getScheme()
    {
        return event.getMessageSourceURI().getScheme();
    }

    @Override
    public String getServerName()
    {
        return message.getInboundProperty(HttpConstants.HEADER_HOST);
    }

    @Override
    public int getServerPort()
    {
        return 0;
    }

    @Override
    public BufferedReader getReader() throws IOException
    {
        return null;
    }

    @Override
    public String getRemoteAddr()
    {
        return null;
    }

    @Override
    public String getRemoteHost()
    {
        return null;
    }

    @Override
    public void setAttribute(String name, Object o)
    {
    }

    @Override
    public void removeAttribute(String name)
    {
    }

    @Override
    public Locale getLocale()
    {
        return null;
    }

    @Override
    public Enumeration getLocales()
    {
        return null;
    }

    @Override
    public boolean isSecure()
    {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path)
    {
        return null;
    }

    @Override
    public String getRealPath(String path)
    {
        return null;
    }

    @Override
    public int getRemotePort()
    {
        return 0;
    }

    @Override
    public String getLocalName()
    {
        return null;
    }

    @Override
    public String getLocalAddr()
    {
        return null;
    }

    @Override
    public int getLocalPort()
    {
        return 0;
    }

    @Override
    public String getAuthType()
    {
        return null;
    }

    @Override
    public Cookie[] getCookies()
    {
        org.apache.commons.httpclient.Cookie[] cookies = message.getInboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        if (cookies == null) return null;

        Cookie[] servletCookies = new Cookie[cookies.length];
        for (org.apache.commons.httpclient.Cookie c : cookies)
        {
            Cookie servletCookie = new Cookie(c.getName(), c.getValue());
            
            servletCookie.setComment(c.getComment());
            servletCookie.setDomain(c.getDomain());
            
        }
        return servletCookies;
    }

    @Override
    public long getDateHeader(String name)
    {
        return 0;
    }

    @Override
    public String getHeader(String name)
    {
        return message.getInboundProperty(name);
    }

    @Override
    public Enumeration getHeaders(String name)
    {
        return new IteratorEnumeration(Arrays.asList(getHeader(name)).iterator());
    }

    @Override
    public Enumeration getHeaderNames()
    {
        Iterator<String> iterator = message.getInboundPropertyNames().iterator();
        return new IteratorEnumeration(iterator);
    }

    @Override
    public int getIntHeader(String name)
    {
        return 0;
    }

    @Override
    public String getMethod()
    {
        return message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY);
    }

    @Override
    public String getPathInfo()
    {
        String req = message.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY);
        String contextPath = message.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);

        String pathInfo = req.substring(contextPath.length());
        if (!pathInfo.startsWith("/")) {
            pathInfo = "/" + pathInfo;
        }
        return pathInfo;
    }

    @Override
    public String getPathTranslated()
    {
        return null;
    }

    @Override
    public String getContextPath()
    {
        return message.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);
    }

    @Override
    public String getQueryString()
    {
        String req = message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        if (req != null) {
            int queryPath = req.indexOf('?');
            if (queryPath > -1) {
                return req.substring(queryPath+1);
            }
        }
        return null;
    }

    @Override
    public String getRemoteUser()
    {
        return null;
    }

    @Override
    public boolean isUserInRole(String role)
    {
        return false;
    }

    @Override
    public Principal getUserPrincipal()
    {
        return null;
    }

    @Override
    public String getRequestedSessionId()
    {
        return null;
    }

    @Override
    public String getRequestURI()
    {
        return message.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY);
    }

    @Override
    public StringBuffer getRequestURL()
    {
        return null;
    }

    @Override
    public String getServletPath()
    {
        // assume contextpath as we have no servlet path
        return message.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);
    }

    @Override
    public HttpSession getSession(boolean create)
    {
        return null;
    }

    @Override
    public HttpSession getSession()
    {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid()
    {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie()
    {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL()
    {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl()
    {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException
    {
        return false;
    }

    @Override
    public ServletContext getServletContext()
    {
        return null;
    }

    @Override
    public void logout() throws ServletException
    {
    }

    @Override
    public void login(String s, String s2) throws ServletException
    {
    }

    @Override
    public AsyncContext startAsync()
    {
        return null;
    }

    @Override
    public boolean isAsyncStarted()
    {
        return false;
    }

    @Override
    public boolean isAsyncSupported()
    {
        return false;
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException
    {
        return null;
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException
    {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
    {
        return null;
    }

    @Override
    public AsyncContext getAsyncContext()
    {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType()
    {
        return null;
    }
}
