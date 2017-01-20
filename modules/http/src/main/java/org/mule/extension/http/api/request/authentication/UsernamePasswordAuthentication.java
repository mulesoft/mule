/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.authentication;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.service.http.api.client.HttpAuthenticationType;
import org.mule.service.http.api.client.HttpRequestAuthentication;
import org.mule.service.http.api.domain.message.request.HttpRequestBuilder;

/**
 * Base class for user/pass based implementations.
 *
 * @since 4.0
 */
public abstract class UsernamePasswordAuthentication implements HttpAuthentication {

  /**
   * The username to authenticate.
   */
  @Parameter
  private String username;

  /**
   * The password to authenticate.
   */
  @Parameter
  @Password
  private String password;

  @Override
  public void authenticate(HttpRequestBuilder builder) throws MuleException {
    // do nothing
  }

  @Override
  public boolean shouldRetry(Result<Object, HttpResponseAttributes> firstAttemptResult) throws MuleException {
    return false;
  }

  public abstract HttpRequestAuthentication buildRequestAuthentication();

  protected HttpRequestAuthentication getBaseRequestAuthentication(HttpAuthenticationType authType) {
    HttpRequestAuthentication requestAuthentication = new HttpRequestAuthentication(authType);
    requestAuthentication.setUsername(username);
    requestAuthentication.setPassword(password);
    return requestAuthentication;
  }
}
