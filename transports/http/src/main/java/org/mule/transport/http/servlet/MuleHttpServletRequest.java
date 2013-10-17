/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.servlet;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

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

    public Object getAttribute(String name)
    {
        return null;
    }

    public Enumeration getAttributeNames()
    {
        return null;
    }

    public String getCharacterEncoding()
    {
        return event.getEncoding();
    }

    public void setCharacterEncoding(String env) throws UnsupportedEncodingException
    {
        message.setEncoding(env);
    }

    public int getContentLength()
    {
        return -1;
    }

    public String getContentType()
    {
        return message.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
    }

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

    public String getParameter(String name)
    {
        return null;
    }

    public Enumeration getParameterNames()
    {
        return null;
    }

    public String[] getParameterValues(String name)
    {
        return null;
    }

    public Map getParameterMap()
    {
        return null;
    }

    public String getProtocol()
    {
        return null;
    }

    public String getScheme()
    {
        return event.getMessageSourceURI().getScheme();
    }

    public String getServerName()
    {
        return message.getInboundProperty(HttpConstants.HEADER_HOST);
    }

    public int getServerPort()
    {
        return 0;
    }

    public BufferedReader getReader() throws IOException
    {
        return null;
    }

    public String getRemoteAddr()
    {
        return null;
    }

    public String getRemoteHost()
    {
        return null;
    }

    public void setAttribute(String name, Object o)
    {
    }

    public void removeAttribute(String name)
    {
    }

    public Locale getLocale()
    {
        return null;
    }

    public Enumeration getLocales()
    {
        return null;
    }

    public boolean isSecure()
    {
        return false;
    }

    public RequestDispatcher getRequestDispatcher(String path)
    {
        return null;
    }

    public String getRealPath(String path)
    {
        return null;
    }

    public int getRemotePort()
    {
        return 0;
    }

    public String getLocalName()
    {
        return null;
    }

    public String getLocalAddr()
    {
        return null;
    }

    public int getLocalPort()
    {
        return 0;
    }

    public String getAuthType()
    {
        return null;
    }

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

    public long getDateHeader(String name)
    {
        return 0;
    }

    public String getHeader(String name)
    {
        return message.getInboundProperty(name);
    }

    public Enumeration getHeaders(String name)
    {
        return new IteratorEnumeration(Arrays.asList(getHeader(name)).iterator());
    }

    public Enumeration getHeaderNames()
    {
        Iterator<String> iterator = message.getInboundPropertyNames().iterator();
        return new IteratorEnumeration(iterator);
    }

    public int getIntHeader(String name)
    {
        return 0;
    }

    public String getMethod()
    {
        return message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY);
    }

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

    public String getPathTranslated()
    {
        return null;
    }

    public String getContextPath()
    {
        return message.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);
    }

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

    public String getRemoteUser()
    {
        return null;
    }

    public boolean isUserInRole(String role)
    {
        return false;
    }

    public Principal getUserPrincipal()
    {
        return null;
    }

    public String getRequestedSessionId()
    {
        return null;
    }

    public String getRequestURI()
    {
        return message.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY);
    }

    public StringBuffer getRequestURL()
    {
        return null;
    }

    public String getServletPath()
    {
        // assume contextpath as we have no servlet path
        return message.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);
    }

    public HttpSession getSession(boolean create)
    {
        return null;
    }

    public HttpSession getSession()
    {
        return null;
    }

    public boolean isRequestedSessionIdValid()
    {
        return false;
    }

    public boolean isRequestedSessionIdFromCookie()
    {
        return false;
    }

    public boolean isRequestedSessionIdFromURL()
    {
        return false;
    }

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
