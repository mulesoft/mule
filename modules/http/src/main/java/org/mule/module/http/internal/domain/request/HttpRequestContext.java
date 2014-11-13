/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.domain.request;

import org.mule.module.http.internal.listener.grizzly.GrizzlyHttpRequestAdapter;

import java.net.InetSocketAddress;

/**
 * Holds the input from an http request.
 */
public class HttpRequestContext
{

    private final InetSocketAddress remoteHostAddress;
    private HttpRequest request;

    public HttpRequestContext(GrizzlyHttpRequestAdapter httpRequest, InetSocketAddress remoteHostAddress)
    {
        this.request = httpRequest;
        this.remoteHostAddress = remoteHostAddress;
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
}
