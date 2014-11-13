/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

public class HttpMessagePropertiesBuilder
{

    private String uri;
    private String method;
    private String protocol;
    private String remoteHostAddress;
    private String listenerPath;

    public HttpMessagePropertiesBuilder setUri(String uri)
    {
        this.uri = uri;
        return this;
    }

    public HttpMessagePropertiesBuilder setMethod(String method)
    {
        this.method = method;
        return this;
    }

    public HttpMessagePropertiesBuilder setProtocol(String protocol)
    {
        this.protocol = protocol;
        return this;
    }

    public HttpMessagePropertiesBuilder setRemoteHostAddress(String remoteHostAddress)
    {
        this.remoteHostAddress = remoteHostAddress;
        return this;
    }

    public HttpMessagePropertiesBuilder setListenerPath(String listenerPath)
    {
        this.listenerPath = listenerPath;
        return this;
    }

    public HttpMessageProperties build()
    {
        return new HttpMessageProperties(uri, method, protocol, remoteHostAddress, listenerPath);
    }
}
