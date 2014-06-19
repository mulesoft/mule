/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

    public static final char PARAMETERS_SEPARATOR = '?';
    private HttpVersion httpversion = null;
    private String method = null;
    private String uri = null;
    private String uriWithoutParams;

    public static RequestLine parseLine(final String l) throws HttpException
    {
        String method;
        String uri;
        String protocol;
        try
        {
            if (l == null)
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
        StringBuilder sb = new StringBuilder(64);
        sb.append(this.method);
        sb.append(" ");
        sb.append(this.uri);
        sb.append(" ");
        sb.append(this.httpversion);
        return sb.toString();
    }

    /**
     * @return the url without the request parameters
     */
    public String getUrlWithoutParams()
    {
        if (this.uriWithoutParams == null)
        {
            uriWithoutParams = getUri();
            int i = uriWithoutParams.indexOf(PARAMETERS_SEPARATOR);
            if (i > -1)
            {
                uriWithoutParams = uriWithoutParams.substring(0, i);
            }
        }
        return uriWithoutParams;
    }
}
