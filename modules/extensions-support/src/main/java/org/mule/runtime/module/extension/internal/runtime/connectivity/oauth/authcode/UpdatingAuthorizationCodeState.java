/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode;

import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.toAuthorizationCodeState;

import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.exception.TokenInvalidatedException;
import org.mule.oauth.client.api.AuthorizationCodeOAuthDancer;
import org.mule.oauth.client.api.listener.AuthorizationCodeListener;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * An implementation of {@link AuthorizationCodeListener} which registers an {@link AuthorizationCodeListener} in order to get
 * updated state when a refresh token operation is completed or the resource is simply re-authorized.
 */
public class UpdatingAuthorizationCodeState
    implements AuthorizationCodeState, org.mule.sdk.api.connectivity.oauth.AuthorizationCodeState {

  private AuthorizationCodeState delegate;
  private boolean invalidated = false;
  private final ResourceOwnerOAuthContext context;
  private AuthorizationCodeOAuthDancer dancer;

  public UpdatingAuthorizationCodeState(AuthorizationCodeConfig config,
                                        AuthorizationCodeOAuthDancer dancer,
                                        ResourceOwnerOAuthContext initialContext,
                                        Consumer<ResourceOwnerOAuthContext> onUpdate) {
    context = initialContext;
    delegate = toAuthorizationCodeState(config, initialContext);
    this.dancer = dancer;
    dancer.addListener(initialContext.getResourceOwnerId(), new AuthorizationCodeListener() {

      @Override
      public void onAuthorizationCompleted(ResourceOwnerOAuthContext context) {
        update(context);
      }

      @Override
      public void onTokenRefreshed(ResourceOwnerOAuthContext context) {
        update(context);
      }

      @Override
      public void onTokenInvalidated() {
        invalidated = true;
        context.setIsTokenInvalidated(true);
        dancer.getContextForResourceOwner(delegate.getResourceOwnerId()).setIsTokenInvalidated(true);
      }

      private void update(ResourceOwnerOAuthContext context) {
        delegate = toAuthorizationCodeState(config, context);
        invalidated = false;
        onUpdate.accept(context);
      }
    });
  }

  @Override
  public String getAccessToken() {
    if (invalidated || context.getIsTokenInvalidated()
        || dancer.getContextForResourceOwner(delegate.getResourceOwnerId()).getIsTokenInvalidated()) {
      throw new TokenInvalidatedException(
                                          "OAuth token for resource owner id " + delegate.getResourceOwnerId()
                                              + " has been invalidated");
    }
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
