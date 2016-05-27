/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener;

import static org.mule.runtime.module.http.internal.HttpParser.decodeQueryString;
import static org.mule.runtime.module.http.internal.HttpParser.decodeUriParams;
import static org.mule.runtime.module.http.internal.HttpParser.extractPath;
import static org.mule.runtime.module.http.internal.HttpParser.extractQueryParams;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.module.http.internal.ParameterMap;
import org.mule.runtime.module.http.internal.domain.request.ClientConnection;
import org.mule.runtime.module.http.internal.domain.request.HttpRequest;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestContext;
import org.mule.runtime.module.http.internal.listener.ListenerPath;

import java.security.cert.Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * Creates {@link HttpRequestAttributes} based on an {@link HttpRequestContext}, it's parts and a {@link ListenerPath}.
 */
public class HttpRequestAttributesBuilder
{
    private HttpRequestContext requestContext;
    private ListenerPath listenerPath;
    private Map<String, DataHandler> parts;

    public HttpRequestAttributesBuilder setRequestContext(HttpRequestContext requestContext)
    {
        this.requestContext = requestContext;
        return this;
    }

    public HttpRequestAttributesBuilder setListenerPath(ListenerPath listenerPath)
    {
        this.listenerPath = listenerPath;
        return this;
    }

    public HttpRequestAttributesBuilder setParts(Map<String, DataHandler> parts)
    {
        this.parts = parts;
        return this;
    }

    public HttpRequestAttributes build()
    {
        String listenerPath = this.listenerPath.getResolvedPath();
        HttpRequest request = requestContext.getRequest();
        String version = request.getProtocol().asString();
        String scheme = requestContext.getScheme();
        String method = request.getMethod();
        String uri = request.getUri();
        String path = extractPath(uri);
        String queryString = extractQueryParams(uri);
        ParameterMap queryParams = decodeQueryString(queryString);
        ParameterMap uriParams = decodeUriParams(listenerPath, path);
        ClientConnection clientConnection = requestContext.getClientConnection();
        String remoteHostAddress = clientConnection.getRemoteHostAddress().toString();
        Certificate clientCertificate = clientConnection.getClientCertificate();
        String relativePath = this.listenerPath.getRelativePath(path);

        final Collection<String> headerNames = request.getHeaderNames();
        Map<String, Object> headers = new HashMap<>();
        for (String headerName : headerNames)
        {
            final Collection<String> values = request.getHeaderValues(headerName);
            if (values.size() == 1)
            {
                headers.put(headerName, values.iterator().next());
            }
            else
            {
                headers.put(headerName, values);
            }
        }
        return new HttpRequestAttributes(headers, parts, listenerPath, relativePath, version, scheme, method,
                                         path, uri, queryString, queryParams, uriParams, remoteHostAddress,
                                         clientCertificate);
    }
}
