/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;

/**
 * Base contract for a {@link ConnectionProviderWrapper} that is OAuth enabled
 *
 * @param <C> the connection's generic type
 * @since 4.3.0
 */
public interface OAuthConnectionProviderWrapper<C> extends ConnectionProviderWrapper<C> {

  /**
   * @return the id of the OAuth resource owner
   */
  String getResourceOwnerId();

  /**
   * @return the configured {@link OAuthGrantType}
   */
  OAuthGrantType getGrantType();

  /**
   * Executes a refresh token for the given {@code resourceOwnerId}
   *
   * @param resourceOwnerId a resource owner Id
   */
  void refreshToken(String resourceOwnerId);

  /**
   * Invalidates the context of the given {@code resourceOwnerId}
   *
   * @param resourceOwnerId
   */
  void invalidate(String resourceOwnerId);
}
