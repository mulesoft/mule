/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.transport.http.i18n.HttpMessages;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.ProtocolException;

/**
 * Defines a HTTP request-line, consisting of method name, URI and protocol.
 */
public class RequestLine
{

    private HttpVersion httpversion = null;
    private String method = null;
    private String uri = null;

    public static RequestLine parseLine(final String l) throws HttpException
    {
        String method;
        String uri;
        String protocol;
        try
        {
            if (l==null)
            {
                throw new ProtocolException(HttpMessages.requestLineIsMalformed(l).getMessage());
            }
            StringTokenizer st = new StringTokenizer(l, " ");
            method = st.nextToken();
            uri = st.nextToken();
            protocol = st.nextToken();
        }
        catch (NoSuchElementException e)
        {
            throw new ProtocolException(HttpMessages.requestLineIsMalformed(l).getMessage());
        }
        return new RequestLine(method, uri, protocol);
    }

    public RequestLine(final String method, final String uri, final HttpVersion httpversion)
    {
        super();
        if (method == null)
        {
            throw new IllegalArgumentException("Method may not be null");
        }
        if (uri == null)
        {
            throw new IllegalArgumentException("URI may not be null");
        }
        if (httpversion == null)
        {
            throw new IllegalArgumentException("HTTP version may not be null");
        }
        this.method = method;
        this.uri = encodeIfNeeded(uri);
        this.httpversion = httpversion;
    }

    public RequestLine(final String method, final String uri, final String httpversion)
        throws ProtocolException
    {
        this(method, uri, HttpVersion.parse(httpversion));
    }

    /*
     * prevents XSS attacks
     */
    private String encodeIfNeeded(String uri)
    {
        if (uri.contains("<") || uri.contains(">"))
        {
            try
            {
                return URLEncoder.encode(uri, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                // This exception will never occur as long as the JRE supports UTF-8
                throw new RuntimeException(e);
            }
        }
        return uri;
    }

    public String getMethod()
    {
        return this.method;
    }

    public HttpVersion getHttpVersion()
    {
        return this.httpversion;
    }

    public String getUri()
    {
        return this.uri;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer(64);
        sb.append(this.method);
        sb.append(" ");
        sb.append(this.uri);
        sb.append(" ");
        sb.append(this.httpversion);
        return sb.toString();
    }
}
