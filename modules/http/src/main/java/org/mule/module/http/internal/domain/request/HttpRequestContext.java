/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.domain.request;

import java.net.InetSocketAddress;

/**
 * Holds the input from an http request.
 */
public class HttpRequestContext
{

    private final InetSocketAddress remoteHostAddress;
    private HttpRequest request;
    private String scheme;

    public HttpRequestContext(HttpRequest httpRequest, InetSocketAddress remoteHostAddress, String scheme)
    {
        this.request = httpRequest;
        this.remoteHostAddress = remoteHostAddress;
        this.scheme = scheme;
    }

    /**
     * @return the http request content
     */
    public HttpRequest getRequest()
    {
        return this.request;
    }

    /**
     * @return the host address from the client
     */
    public InetSocketAddress getRemoteHostAddress()
    {
        return remoteHostAddress;
    }

    /**
     * @return The scheme of the HTTP request URL (http or https)
     */
    public String getScheme()
    {
        return scheme;
    }
}
