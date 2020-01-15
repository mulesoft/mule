/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static java.util.Optional.empty;

import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.oauth.api.PlatformManagedConnectionDescriptor;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.Optional;
import java.util.function.Consumer;

public class PlatformAuthorizationCodeStateAdapter extends BasePlatformOAuthStateAdapter implements AuthorizationCodeState {

  private final PlatformManagedConnectionDescriptor descriptor;

  public PlatformAuthorizationCodeStateAdapter(PlatformManagedOAuthDancer dancer,
                                               PlatformManagedConnectionDescriptor descriptor,
                                               Consumer<ResourceOwnerOAuthContext> onUpdate) {
    super(dancer, onUpdate);
    this.descriptor = descriptor;
  }

  @Override
  public Optional<String> getRefreshToken() {
    return empty();
  }

  @Override
  public String getResourceOwnerId() {
    return descriptor.getName();
  }

  @Override
  public Optional<String> getState() {
    return empty();
  }

  @Override
  public String getAuthorizationUrl() {
    return "";
  }

  @Override
  public String getAccessTokenUrl() {
    return "";
  }

  @Override
  public String getConsumerKey() {
    return "";
  }

  @Override
  public String getConsumerSecret() {
    return "";
  }

  @Override
  public Optional<String> getExternalCallbackUrl() {
    return empty();
  }
}
