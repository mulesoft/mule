/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static java.util.Optional.ofNullable;

import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.listener.PlatformManagedOAuthStateListener;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Base class for an adapter that bridges implementations of {@link OAuthState} with {@link ResourceOwnerOAuthContext} instances
 * obtained through a {@link PlatformManagedOAuthDancer}
 *
 * @since 4.3.0
 */
abstract class AbstractPlatformOAuthStateAdapter implements OAuthState {

  private String accessToken;
  private Optional<String> expiresIn;

  public AbstractPlatformOAuthStateAdapter(PlatformManagedOAuthDancer dancer, Consumer<ResourceOwnerOAuthContext> onUpdate) {
    updateState(dancer.getContext());
    dancer.addListener(new PlatformManagedOAuthStateListener() {

      @Override
      public void onAccessToken(ResourceOwnerOAuthContext context) {
        doUpdate(context);
      }

      @Override
      public void onTokenRefreshed(ResourceOwnerOAuthContext context) {
        doUpdate(context);
      }

      private void doUpdate(ResourceOwnerOAuthContext context) {
        updateState(context);
        onUpdate.accept(context);
      }
    });
  }

  private void updateState(ResourceOwnerOAuthContext context) {
    accessToken = context.getAccessToken();
    expiresIn = ofNullable(context.getExpiresIn());
  }

  @Override
  public final String getAccessToken() {
    return accessToken;
  }

  @Override
  public final Optional<String> getExpiresIn() {
    return expiresIn;
  }
}
