/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.listener.PlatformManagedOAuthStateListener;

/**
 * Builder that allows to configure the attributes for the {@link PlatformManagedOAuthDancer}
 * <p>
 * Platform Managed OAuth is an experimental feature. It will only be enabled on selected environments and scenarios.
 * Backwards compatibility is not guaranteed.
 *
 * @since 4.3.0
 */
@NoImplement
@Experimental
public interface OAuthPlatformManagedDancerBuilder extends OAuthDancerBuilder<PlatformManagedOAuthDancer> {

  /**
   * Sets the ID of the connection that is defined in the Anypoint Platform
   *
   * @param connectionId the id of the connection which token we want to obtain
   * @return {@code this} builder
   */
  OAuthPlatformManagedDancerBuilder connectionId(String connectionId);

  OAuthPlatformManagedDancerBuilder organizationId(String organizationId);

  OAuthPlatformManagedDancerBuilder platformUrl(String platformUrl);

  /**
   * Adds the {@code listener}. Listeners will be invoked in the same order as they were added
   *
   * @param listener the {@link PlatformManagedOAuthStateListener} to be added
   * @throws IllegalArgumentException if the {@code listener} is {@code null}
   */
  OAuthPlatformManagedDancerBuilder addListener(PlatformManagedOAuthStateListener listener);
}
