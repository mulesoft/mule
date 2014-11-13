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
import java.util.regex.Pattern;

public class HttpMessageProperties
{

    private final String remoteHostAddress;
    private final String listenerPath;
    private final String method;
    private final String protocol;
    private final String uri;
    private ParameterMap queryParams;
    private String rawQueryString;
    private String path;
    private ParameterMap uriParams;

    public HttpMessageProperties(String uri, String method, String protocol, String remoteHostAddress, String listenerPath)
    {
        this.uri = uri;
        this.method = method;
        this.protocol = protocol;
        this.remoteHostAddress = remoteHostAddress;
        this.listenerPath = listenerPath;
    }

    public String getMethod()
    {
        return this.method;
    }

    public String getRawQueryString()
    {
        if (rawQueryString == null)
        {
            this.rawQueryString = HttpParser.extractQueryParams(uri);
        }
        return rawQueryString;
    }

    public String getPath()
    {
        if (this.path == null)
        {
            this.path = HttpParser.extractPath(uri);
        }
        return path;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public ParameterMap getQueryParams()
    {
        if (queryParams == null)
        {
            queryParams = HttpParser.decodeQueryString(getRawQueryString());
        }
        return queryParams;
    }

    public String getUri()
    {
        return uri;
    }

    public void addPropertiesTo(Map<String, Object> propertiesMap)
    {
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_METHOD_PROPERTY, getMethod());
        final ParameterMap queryParams = getQueryParams();
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_QUERY_PARAMS, queryParams == null ? Collections.emptyMap() : queryParams);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_QUERY_STRING, getRawQueryString());
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY, getPath());
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_VERSION_PROPERTY, getProtocol());
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_REQUEST_URI, getUri());
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_REMOTE_ADDRESS, remoteHostAddress);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_URI_PARAMS, getUriParams());
    }

    public ParameterMap getUriParams()
    {
        if (uriParams == null)
        {
            uriParams = new ParameterMap();
            if (this.listenerPath.contains("{"))
            {
                final String[] requestPathParts = this.getPath().split("/");
                final String[] listenerPathParts = this.listenerPath.split("/");
                int longerPathSize = Math.min(requestPathParts.length, listenerPathParts.length);
                //split will return an empty string as first path before /
                for (int i = 1; i < longerPathSize; i++)
                {
                    final String listenerPart = listenerPathParts[i];
                    if (listenerPart.startsWith("{") && listenerPart.endsWith("}"))
                    {
                        String parameterName = listenerPart.substring(1, listenerPart.length() - 1);
                        String parameterValue = requestPathParts[i];
                        uriParams.put(parameterName, parameterValue);
                    }
                }
            }
        }
        return uriParams;
    }
}
