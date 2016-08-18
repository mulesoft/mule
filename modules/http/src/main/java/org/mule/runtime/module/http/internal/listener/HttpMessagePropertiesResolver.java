/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import static org.mule.runtime.module.http.internal.HttpParser.decodeQueryString;
import static org.mule.runtime.module.http.internal.HttpParser.decodeUriParams;
import static org.mule.runtime.module.http.internal.HttpParser.extractPath;
import static org.mule.runtime.module.http.internal.HttpParser.extractQueryParams;

import org.mule.service.http.api.domain.ParameterMap;
import org.mule.runtime.module.http.api.HttpConstants;

import java.io.Serializable;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;

public class HttpMessagePropertiesResolver {

  private String uri;
  private String method;
  private String protocol;
  private String remoteHostAddress;
  private ListenerPath listenerPath;
  private String scheme;
  private Certificate clientCertificate;

  public HttpMessagePropertiesResolver setUri(String uri) {
    this.uri = uri;
    return this;
  }

  public HttpMessagePropertiesResolver setMethod(String method) {
    this.method = method;
    return this;
  }

  public HttpMessagePropertiesResolver setProtocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  public HttpMessagePropertiesResolver setRemoteHostAddress(String remoteHostAddress) {
    this.remoteHostAddress = remoteHostAddress;
    return this;
  }

  public HttpMessagePropertiesResolver setListenerPath(ListenerPath listenerPath) {
    this.listenerPath = listenerPath;
    return this;
  }

  public HttpMessagePropertiesResolver setScheme(String scheme) {
    this.scheme = scheme;
    return this;
  }

  public HttpMessagePropertiesResolver setClientCertificate(Certificate clientCertificate) {
    this.clientCertificate = clientCertificate;
    return this;
  }

  public void addPropertiesTo(Map<String, Serializable> propertiesMap) {
    final String resolvedListenerPath = listenerPath.getResolvedPath();
    propertiesMap.put(HttpConstants.RequestProperties.HTTP_METHOD_PROPERTY, method);
    final String path = extractPath(uri);
    final String rawQueryString = extractQueryParams(uri);
    final ParameterMap queryParams = decodeQueryString(rawQueryString);
    propertiesMap.put(HttpConstants.RequestProperties.HTTP_QUERY_PARAMS,
                      queryParams == null ? new HashMap<>() : queryParams.toImmutableParameterMap());
    propertiesMap.put(HttpConstants.RequestProperties.HTTP_QUERY_STRING, rawQueryString);
    propertiesMap.put(HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY, path);
    propertiesMap.put(HttpConstants.RequestProperties.HTTP_VERSION_PROPERTY, protocol);
    propertiesMap.put(HttpConstants.RequestProperties.HTTP_REQUEST_URI, uri);
    propertiesMap.put(HttpConstants.RequestProperties.HTTP_REMOTE_ADDRESS, remoteHostAddress);
    propertiesMap.put(HttpConstants.RequestProperties.HTTP_URI_PARAMS, decodeUriParams(resolvedListenerPath, path));
    propertiesMap.put(HttpConstants.RequestProperties.HTTP_LISTENER_PATH, resolvedListenerPath);
    propertiesMap.put(HttpConstants.RequestProperties.HTTP_RELATIVE_PATH, listenerPath.getRelativePath(path));
    propertiesMap.put(HttpConstants.RequestProperties.HTTP_SCHEME, scheme);
    if (clientCertificate != null) {
      propertiesMap.put(HttpConstants.RequestProperties.HTTP_CLIENT_CERTIFICATE, clientCertificate);
    }
  }
}
