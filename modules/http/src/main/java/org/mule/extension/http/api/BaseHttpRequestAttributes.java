/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import org.mule.service.http.api.domain.ParameterMap;

/**
 * Common parts of all http request attributes.
 *
 * @since 4.0
 */
public class BaseHttpRequestAttributes extends HttpAttributes {

  /**
   * Query parameters map built from the parsed string. Former 'http.query.params'.
   */
  protected ParameterMap queryParams;
  /**
   * URI parameters extracted from the request path. Former 'http.uri.params'.
   */
  protected ParameterMap uriParams;

  /**
   * Full path requested. Former 'http.request.path'.
   */
  protected String requestPath;

  public BaseHttpRequestAttributes(ParameterMap headers, ParameterMap queryParams, ParameterMap uriParams, String requestPath) {
    super(headers);
    this.queryParams = queryParams;
    this.uriParams = uriParams;
    this.requestPath = requestPath;
  }

  public String getRequestPath() {
    return requestPath;
  }

  public ParameterMap getQueryParams() {
    return queryParams;
  }

  public ParameterMap getUriParams() {
    return uriParams;
  }

}
