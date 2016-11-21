/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.authentication;

import org.mule.extension.http.internal.request.HttpRequestBuilder;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestAuthentication;
import org.mule.runtime.module.http.internal.request.HttpAuthenticationType;

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
  public void authenticate(Event muleEvent, HttpRequestBuilder builder) throws MuleException {
    // do nothing
  }

  @Override
  public boolean shouldRetry(Event firstAttemptResponseEvent) throws MuleException {
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
