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
        String method = null;
        String uri = null;
        String protocol = null;
        try
        {
            if(l==null)
            {
                System.out.println("");
            }
            StringTokenizer st = new StringTokenizer(l, " ");
            method = st.nextToken();
            uri = st.nextToken();
            protocol = st.nextToken();
        }
        catch (NoSuchElementException e)
        {
            throw new ProtocolException("Invalid request line: " + l);
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
        this.uri = uri;
        this.httpversion = httpversion;
    }

    public RequestLine(final String method, final String uri, final String httpversion)
        throws ProtocolException
    {
        this(method, uri, HttpVersion.parse(httpversion));
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
