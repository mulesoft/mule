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
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * Common interface for all grant types must extend this interface.
 * 
 * @since 4.0
 */
// TODO MULE-11412 Remove MuleContextAware
public abstract class AbstractGrantType implements HttpAuthentication, ApplicationCredentials, MuleContextAware {

  // TODO MULE-11412 Add @Inject
  protected MuleContext muleContext;

  protected DeferredExpressionResolver resolver;

  /**
   * The token manager configuration to use for this grant type.
   */
  @Parameter
  @Optional
  protected TokenManagerConfig tokenManager;

  /**
   * @param accessToken an oauth access token
   * @return the content of the HTTP authentication header.
   */
  protected String buildAuthorizationHeaderContent(String accessToken) {
    return "Bearer " + accessToken;
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
    this.resolver = new DeferredExpressionResolver(muleContext);
  }
}
