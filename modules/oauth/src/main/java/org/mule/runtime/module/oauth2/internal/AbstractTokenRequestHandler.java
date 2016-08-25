/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.oauth2.internal;

import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTokenRequestHandler implements MuleContextAware {

  protected Logger logger = LoggerFactory.getLogger(getClass());
  protected MuleContext muleContext;
  private String refreshTokenWhen = OAuthConstants.DEFAULT_REFRESH_TOKEN_WHEN_EXPRESSION;
  private String tokenUrl;
  private HttpRequestOptions httpRequestOptions =
      HttpRequestOptionsBuilder.newOptions().method(POST.name()).disableStatusCodeValidation().build();
  private TlsContextFactory tlsContextFactory;

  /**
   * @param refreshTokenWhen expression to use to determine if the response from a request to the API requires a new token
   */
  public void setRefreshTokenWhen(String refreshTokenWhen) {
    this.refreshTokenWhen = refreshTokenWhen;
  }

  public String getRefreshTokenWhen() {
    return refreshTokenWhen;
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  protected MuleContext getMuleContext() {
    return muleContext;
  }

  public void setTokenUrl(String tokenUrl) {
    this.tokenUrl = tokenUrl;
  }

  public void setTlsContextFactory(final TlsContextFactory tlsContextFactory) {
    httpRequestOptions = HttpRequestOptionsBuilder.newOptions().method(POST.name()).disableStatusCodeValidation()
        .tlsContextFactory(tlsContextFactory).build();
  }

  protected MuleEvent invokeTokenUrl(final MuleEvent event) throws MuleException, TokenUrlResponseException {
    event.setMessage(muleContext.getClient().send(tokenUrl, event.getMessage(), httpRequestOptions).getRight());
    if (event.getMessage().<Integer>getInboundProperty(HTTP_STATUS_PROPERTY) >= BAD_REQUEST.getStatusCode()) {
      throw new TokenUrlResponseException(event);
    }
    return event;
  }

  protected String getTokenUrl() {
    return tokenUrl;
  }

  protected class TokenUrlResponseException extends Exception {

    private MuleEvent tokenUrlResponse;

    public TokenUrlResponseException(final MuleEvent tokenUrlResponse) {
      this.tokenUrlResponse = tokenUrlResponse;
    }

    public MuleEvent getTokenUrlResponse() {
      return tokenUrlResponse;
    }
  }
}
