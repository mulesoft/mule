/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import static java.util.Collections.unmodifiableMap;

import java.security.cert.Certificate;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * Representation of an HTTP request message attributes.
 */
public class HttpRequestAttributes extends HttpAttributes
{
    private final String listenerPath;
    private final String relativePath;
    private final String version;
    private final String scheme;
    private final String method;
    private final String requestPath;
    private final String requestUri;
    private final String queryString;
    private final Map<String, String> queryParams;
    private final Map<String, String> uriParams;
    private final String remoteHostAddress;
    private final Certificate clientCertificate;

    public HttpRequestAttributes(Map<String, Object> headers, Map<String, DataHandler> parts, String listenerPath,
                                 String relativePath, String version, String scheme, String method, String requestPath,
                                 String requestUri, String queryString, Map<String, String> queryParams,
                                 Map<String, String> uriParams, String remoteHostAddress, Certificate clientCertificate)
    {
        super(unmodifiableMap(headers), unmodifiableMap(parts));
        this.listenerPath = listenerPath;
        this.relativePath = relativePath;
        this.version = version;
        this.scheme = scheme;
        this.method = method;
        this.requestPath = requestPath;
        this.requestUri = requestUri;
        this.queryString = queryString;
        this.queryParams = unmodifiableMap(queryParams);
        this.uriParams = unmodifiableMap(uriParams);
        this.remoteHostAddress = remoteHostAddress;
        this.clientCertificate = clientCertificate;
    }

    public String getListenerPath()
    {
        return listenerPath;
    }

    public String getRelativePath()
    {
        return relativePath;
    }

    public String getVersion()
    {
        return version;
    }

    public String getScheme()
    {
        return scheme;
    }

    public String getMethod()
    {
        return method;
    }

    public String getRequestPath()
    {
        return requestPath;
    }

    public String getRequestUri()
    {
        return requestUri;
    }

    public String getQueryString()
    {
        return queryString;
    }

    public Map<String, String> getQueryParams()
    {
        return queryParams;
    }

    public Map<String, String> getUriParams()
    {
        return uriParams;
    }

    public String getRemoteHostAddress()
    {
        return remoteHostAddress;
    }

    public Certificate getClientCertificate()
    {
        return clientCertificate;
    }

}
