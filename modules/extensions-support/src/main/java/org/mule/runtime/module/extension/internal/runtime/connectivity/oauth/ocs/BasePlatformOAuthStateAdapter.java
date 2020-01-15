/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static java.util.Optional.ofNullable;

import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.listener.PlatformManagedOAuthStateListener;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.Optional;
import java.util.function.Consumer;

abstract class BasePlatformOAuthStateAdapter implements OAuthState {

  private String accessToken;
  private Optional<String> expiresIn;

  public BasePlatformOAuthStateAdapter(PlatformManagedOAuthDancer dancer, Consumer<ResourceOwnerOAuthContext> onUpdate) {
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
