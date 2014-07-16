/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.servlet;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;


/**
 * THIS CLASS IS UNSUPPORTED AND THE IMPLEMENTATION DOES NOT CONFORM TO THE SERVLET SPECIFICATION!
 */
public class MuleHttpServletResponse implements HttpServletResponse
{
    private static String[] DAYS =
    { "Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static String[] MONTHS =
    { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan"};

    private MuleEvent event;
    private MuleMessage message;

    public MuleHttpServletResponse(MuleEvent event)
    {
        super();
        this.event = event;
        this.message = event.getMessage();
    }
    
    public String getCharacterEncoding()
    {
        return event.getEncoding();
    }

    public String getContentType()
    {
        return message.getOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
    }

    public ServletOutputStream getOutputStream() throws IOException
    {
        throw new UnsupportedOperationException();
    }

    public PrintWriter getWriter() throws IOException
    {
        throw new UnsupportedOperationException();
    }

    public void setCharacterEncoding(String charset)
    {
        message.setEncoding(charset);
    }

    public void setContentLength(int len)
    {
        throw new UnsupportedOperationException();
    }

    public void setContentType(String type)
    {
        message.setProperty(HttpConstants.HEADER_CONTENT_TYPE, type, PropertyScope.OUTBOUND);
    }

    public void setBufferSize(int size)
    {
    }

    public int getBufferSize()
    {
        return 0;
    }

    public void flushBuffer() throws IOException
    {
    }

    public void resetBuffer()
    {
    }

    public boolean isCommitted()
    {
        return false;
    }

    public void reset()
    {
    }

    public void setLocale(Locale loc)
    {
    }

    public Locale getLocale()
    {
        return null;
    }

    public void addCookie(Cookie cookie)
    {
        org.apache.commons.httpclient.Cookie internal = toHttpClientCookie(cookie);
        
        org.apache.commons.httpclient.Cookie[] internalCookies = message.getOutboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        if (internalCookies == null)
        {
            internalCookies = new org.apache.commons.httpclient.Cookie[] { internal };
        }
        else
        {
            List<org.apache.commons.httpclient.Cookie> list = new ArrayList<org.apache.commons.httpclient.Cookie>(Arrays.asList(internalCookies));
            list.add(internal);
            internalCookies = list.toArray(new org.apache.commons.httpclient.Cookie[list.size()]);
        }
        message.setOutboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY, internalCookies);
    }

    private org.apache.commons.httpclient.Cookie toHttpClientCookie(Cookie cookie)
    {
        org.apache.commons.httpclient.Cookie internal = new org.apache.commons.httpclient.Cookie();
        
        internal.setName(cookie.getName());
        internal.setValue(cookie.getValue());
        internal.setComment(cookie.getComment());
        internal.setDomain(cookie.getDomain());
//        internal.setExpiryDate(toExpiry(cookie.getMaxAge()));
        internal.setPath(cookie.getPath());
        internal.setVersion(cookie.getVersion());
        
        return internal;
    }

    public boolean containsHeader(String name)
    {
        return message.getOutboundProperty(name) != null;
    }

    public String encodeURL(String url)
    {
        return null;
    }

    public String encodeRedirectURL(String url)
    {
        return null;
    }

    public String encodeUrl(String url)
    {
        return null;
    }

    public String encodeRedirectUrl(String url)
    {
        return null;
    }

    public void sendError(int sc, String msg) throws IOException
    {
    }

    public void sendError(int sc) throws IOException
    {
    }

    public void sendRedirect(String location) throws IOException
    {
        setStatus(302);
        setHeader("Location", location);
    }

    public void setDateHeader(String name, long date)
    {
        StringBuilder buf = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        formatDate(buf, calendar, false);
        message.setOutboundProperty(name, buf.toString());
    }

    public void addDateHeader(String name, long date)
    {
        setDateHeader(name, date);
    }

    /**
     * Format HTTP date "EEE, dd MMM yyyy HH:mm:ss 'GMT'" or "EEE, dd-MMM-yy HH:mm:ss 'GMT'"for
     * cookies
     */
    public static void formatDate(StringBuilder buf, Calendar calendar, boolean cookie)
    {
        // "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
        // "EEE, dd-MMM-yy HH:mm:ss 'GMT'", cookie

        int day_of_week = calendar.get(Calendar.DAY_OF_WEEK);
        int day_of_month = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        int century = year / 100;
        year = year % 100;

        int epoch = (int) ((calendar.getTimeInMillis() / 1000) % (60 * 60 * 24));
        int seconds = epoch % 60;
        epoch = epoch / 60;
        int minutes = epoch % 60;
        int hours = epoch / 60;

        buf.append(DAYS[day_of_week]);
        buf.append(',');
        buf.append(' ');
        append2digits(buf, day_of_month);

        if (cookie)
        {
            buf.append('-');
            buf.append(MONTHS[month]);
            buf.append('-');
            append2digits(buf, century);
            append2digits(buf, year);
        }
        else
        {
            buf.append(' ');
            buf.append(MONTHS[month]);
            buf.append(' ');
            append2digits(buf, century);
            append2digits(buf, year);
        }
        buf.append(' ');
        append2digits(buf, hours);
        buf.append(':');
        append2digits(buf, minutes);
        buf.append(':');
        append2digits(buf, seconds);
        buf.append(" GMT");
    }

    public static void append2digits(StringBuilder buf, int i)
    {
        if (i >= 100)
            return;
        buf.append((char) (i / 10 + 48));
        buf.append((char) (i % 10 + 48));
    }
    
    public void setHeader(String name, String value)
    {
        message.setOutboundProperty(name, value);
    }

    public void addHeader(String name, String value)
    {
        message.setOutboundProperty(name, value);
    }

    public void setIntHeader(String name, int value)
    {
        message.setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, value);
    }

    public void addIntHeader(String name, int value)
    {
        message.setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, value);
    }

    public void setStatus(int sc)
    {
        message.setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, sc);
    }

    public void setStatus(int sc, String sm)
    {
        message.setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, sc);
    }

    @Override
    public Collection<String> getHeaderNames()
    {
        return null;
    }

    @Override
    public int getStatus()
    {
        return 0;
    }

    @Override
    public Collection<String> getHeaders(String s)
    {
        return null;
    }

    @Override
    public String getHeader(String s)
    {
        return null;
    }
}
