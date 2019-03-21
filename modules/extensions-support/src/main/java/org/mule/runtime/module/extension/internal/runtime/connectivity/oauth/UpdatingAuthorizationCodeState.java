/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.toAuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.api.builder.AuthorizationCodeListener;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * An implementation of {@link AuthorizationCodeListener} which registers an {@link AuthorizationCodeListener}
 * in order to get updated state when a refresh token operation is completed or the resource is simply re-authorized.
 * 
 */
public class UpdatingAuthorizationCodeState implements AuthorizationCodeState {

  private AuthorizationCodeState delegate;
  private final Consumer<ResourceOwnerOAuthContext> onUpdate;

  public UpdatingAuthorizationCodeState(OAuthConfig config,
                                        AuthorizationCodeOAuthDancer dancer,
                                        ResourceOwnerOAuthContext initialContext,
                                        Consumer<ResourceOwnerOAuthContext> onUpdate) {
    delegate = toAuthorizationCodeState(config, initialContext);
    this.onUpdate = onUpdate;
    dancer.addListener(new AuthorizationCodeListener() {

      @Override
      public void onAuthorizationCompleted(ResourceOwnerOAuthContext context) {
        update(context);
      }

      @Override
      public void onTokenRefreshed(ResourceOwnerOAuthContext context) {
        update(context);
      }

      private void update(ResourceOwnerOAuthContext context) {
        delegate = toAuthorizationCodeState(config, context);
        onUpdate.accept(context);
      }
    });
  }

  @Override
  public String getAccessToken() {
    return delegate.getAccessToken();
  }

  @Override
  public Optional<String> getRefreshToken() {
    return delegate.getRefreshToken();
  }

  @Override
  public String getResourceOwnerId() {
    return delegate.getResourceOwnerId();
  }

  @Override
  public Optional<String> getExpiresIn() {
    return delegate.getExpiresIn();
  }

  @Override
  public Optional<String> getState() {
    return delegate.getState();
  }

  @Override
  public String getAuthorizationUrl() {
    return delegate.getAuthorizationUrl();
  }

  @Override
  public String getAccessTokenUrl() {
    return delegate.getAccessTokenUrl();
  }

  @Override
  public String getConsumerKey() {
    return delegate.getConsumerKey();
  }

  @Override
  public String getConsumerSecret() {
    return delegate.getConsumerSecret();
  }

  @Override
  public Optional<String> getExternalCallbackUrl() {
    return delegate.getExternalCallbackUrl();
  }
}
