/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import org.mule.service.http.api.domain.ParameterMap;

import java.security.cert.Certificate;

/**
 * Representation of an HTTP request message attributes.
 *
 * @since 4.0
 */
public class HttpRequestAttributes extends BaseHttpRequestAttributes {

  /**
   * Full path where the request was received. Former 'http.listener.path'.
   */
  private String listenerPath;

  /**
   * Path where the request was received, without considering the base path. Former 'http.relative.path'.
   */
  private String relativePath;
  /**
   * HTTP version of the request. Former 'http.version'.
   */
  private String version;
  /**
   * HTTP scheme of the request. Former 'http.scheme'.
   */
  private String scheme;
  /**
   * HTTP method of the request. Former 'http.method'.
   */
  private String method;
  /**
   * Full URI of the request. Former 'http.request.uri'.
   */
  private String requestUri;
  /**
   * Query string of the request. Former 'http.query.string'.
   */
  private String queryString;
  /**
   * Remote host address from the sender. Former 'http.remote.address'.
   */
  private String remoteAddress;
  /**
   * Client certificate (if 2 way TLS is enabled). Former 'http.client.cert'.
   */
  private Certificate clientCertificate;

  public HttpRequestAttributes(ParameterMap headers, String listenerPath, String relativePath, String version, String scheme,
                               String method, String requestPath, String requestUri, String queryString, ParameterMap queryParams,
                               ParameterMap uriParams, String remoteAddress, Certificate clientCertificate) {
    super(headers, queryParams, uriParams, requestPath);
    this.listenerPath = listenerPath;
    this.relativePath = relativePath;
    this.version = version;
    this.scheme = scheme;
    this.method = method;
    this.requestUri = requestUri;
    this.queryString = queryString;
    this.remoteAddress = remoteAddress;
    this.clientCertificate = clientCertificate;
  }

  public String getListenerPath() {
    return listenerPath;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public String getVersion() {
    return version;
  }

  public String getScheme() {
    return scheme;
  }

  public String getMethod() {
    return method;
  }

  public String getRequestUri() {
    return requestUri;
  }

  public String getQueryString() {
    return queryString;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public Certificate getClientCertificate() {
    return clientCertificate;
  }

  public void setListenerPath(String listenerPath) {
    this.listenerPath = listenerPath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public void setRequestUri(String requestUri) {
    this.requestUri = requestUri;
  }

  public void setQueryString(String queryString) {
    this.queryString = queryString;
  }

  public void setRemoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
  }

  public void setClientCertificate(Certificate clientCertificate) {
    this.clientCertificate = clientCertificate;
  }

}
