/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.ParameterMap;

import java.util.Collections;
import java.util.Map;

public class HttpMessagePropertiesResolver
{

    private String uri;
    private String method;
    private String protocol;
    private String remoteHostAddress;
    private String listenerPath;
    private String scheme;

    public HttpMessagePropertiesResolver setUri(String uri)
    {
        this.uri = uri;
        return this;
    }

    public HttpMessagePropertiesResolver setMethod(String method)
    {
        this.method = method;
        return this;
    }

    public HttpMessagePropertiesResolver setProtocol(String protocol)
    {
        this.protocol = protocol;
        return this;
    }

    public HttpMessagePropertiesResolver setRemoteHostAddress(String remoteHostAddress)
    {
        this.remoteHostAddress = remoteHostAddress;
        return this;
    }

    public HttpMessagePropertiesResolver setListenerPath(String listenerPath)
    {
        this.listenerPath = listenerPath;
        return this;
    }

    public HttpMessagePropertiesResolver setScheme(String scheme)
    {
        this.scheme = scheme;
        return this;
    }

    public void addPropertiesTo(Map<String, Object> propertiesMap)
    {
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_METHOD_PROPERTY, this.method);
        final String path = HttpParser.extractPath(uri);
        final String rawQueryString = HttpParser.extractQueryParams(uri);
        final ParameterMap queryParams = HttpParser.decodeQueryString(rawQueryString);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_QUERY_PARAMS, queryParams == null ? Collections.emptyMap() : queryParams);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_QUERY_STRING, rawQueryString);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY, path);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_VERSION_PROPERTY, this.protocol);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_REQUEST_URI, this.uri);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_REMOTE_ADDRESS, remoteHostAddress);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_URI_PARAMS, HttpParser.decodeUriParams(this.listenerPath, path));
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_LISTENER_PATH, listenerPath);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_SCHEME, scheme);
    }



}
