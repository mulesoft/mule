/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import org.mule.extension.http.api.BaseHttpRequestAttributes;
import org.mule.service.http.api.domain.ParameterMap;

import java.util.Map;

/**
 * {@link BaseHttpRequestAttributes} subclass that allows modification of request attributes and
 * creation through the expression language.
 *
 * @since 4.0
 */
public class HttpPolicyRequestAttributes extends BaseHttpRequestAttributes {

  public HttpPolicyRequestAttributes(ParameterMap headers, ParameterMap queryParams, ParameterMap uriParams, String requestPath) {
    super(headers, queryParams, uriParams, requestPath);
  }

  public HttpPolicyRequestAttributes() {
    super(new ParameterMap(), new ParameterMap(), new ParameterMap(), "");
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = new ParameterMap(headers);
  }

  public void setQueryParams(Map<String, String> queryParams) {
    this.queryParams = new ParameterMap(queryParams);
  }

  public void setUriParams(Map<String, String> uriParams) {
    this.uriParams = new ParameterMap(uriParams);
  }

}
