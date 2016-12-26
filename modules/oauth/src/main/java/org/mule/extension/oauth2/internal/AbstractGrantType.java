/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * Common interface for all grant types must extend this interface.
 */
public abstract class AbstractGrantType implements HttpAuthentication, ApplicationCredentials, MuleContextAware {

  protected MuleContext muleContext;

  /**
   * The token manager configuration to use for this grant type.
   */
  @Parameter
  @Optional
  @Alias("tokenManager-ref")
  protected TokenManagerConfig tokenManager;

  /**
   * @param accessToken an ouath access token
   * @return the content of the HTTP authentication header.
   */
  public static String buildAuthorizationHeaderContent(String accessToken) {
    return "Bearer " + accessToken;
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
