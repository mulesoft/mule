/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.authentication;

import static org.mule.runtime.module.http.internal.request.HttpAuthenticationType.BASIC;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestAuthentication;

/**
 * Configures basic authentication for the requests.
 *
 * @since 4.0
 */
public class BasicAuthentication extends UsernamePasswordAuthentication {

  /**
   * Configures if authentication should be preemptive or not. Preemptive authentication will send the authentication header in
   * the first request, instead of waiting for a 401 response code to send it.
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean preemptive;

  @Override
  public HttpRequestAuthentication buildRequestAuthentication() {
    HttpRequestAuthentication requestAuthentication = getBaseRequestAuthentication(BASIC);
    requestAuthentication.setPreemptive(preemptive);
    return requestAuthentication;
  }
}
