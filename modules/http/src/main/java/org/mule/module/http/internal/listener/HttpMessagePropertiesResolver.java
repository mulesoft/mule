/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static java.util.Collections.emptyMap;
import static org.mule.module.http.internal.HttpParser.decodeQueryString;
import static org.mule.module.http.internal.HttpParser.decodeUriParams;
import static org.mule.module.http.internal.HttpParser.extractPath;
import static org.mule.module.http.internal.HttpParser.extractQueryParams;

import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.internal.ParameterMap;

import java.security.cert.Certificate;
import java.util.Map;

import javax.net.ssl.SSLSession;

public class HttpMessagePropertiesResolver
{

    private String uri;
    private String method;
    private String protocol;
    private String remoteHostAddress;
    private ListenerPath listenerPath;
    private String scheme;
    private Certificate clientCertificate;
    private SSLSession sslSession;

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

    public HttpMessagePropertiesResolver setListenerPath(ListenerPath listenerPath)
    {
        this.listenerPath = listenerPath;
        return this;
    }
    
    public HttpMessagePropertiesResolver setSslSession(SSLSession sslSession)
    {
        this.sslSession = sslSession;
        return this;
    }

    public HttpMessagePropertiesResolver setScheme(String scheme)
    {
        this.scheme = scheme;
        return this;
    }

    public HttpMessagePropertiesResolver setClientCertificate(Certificate clientCertificate)
    {
        this.clientCertificate = clientCertificate;
        return this;
    }

    public void addPropertiesTo(Map<String, Object> propertiesMap)
    {
        final String resolvedListenerPath = listenerPath.getResolvedPath();
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_METHOD_PROPERTY, method);
        final String path = extractPath(uri);
        final String rawQueryString = extractQueryParams(uri);
        final ParameterMap queryParams = decodeQueryString(rawQueryString);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_QUERY_PARAMS, queryParams == null ? emptyMap() : queryParams.toImmutableParameterMap());
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_QUERY_STRING, rawQueryString);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY, path);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_VERSION_PROPERTY, protocol);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_REQUEST_URI, uri);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_REMOTE_ADDRESS, remoteHostAddress);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_URI_PARAMS, decodeUriParams(resolvedListenerPath, path));
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_LISTENER_PATH, resolvedListenerPath);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_RELATIVE_PATH, listenerPath.getRelativePath(path));
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_SCHEME, scheme);
        propertiesMap.put(HttpConstants.RequestProperties.HTTP_CLIENT_SSL_SESSION, sslSession);
        if (clientCertificate != null)
        {
            propertiesMap.put(HttpConstants.RequestProperties.HTTP_CLIENT_CERTIFICATE, clientCertificate);
        }
    }
}
