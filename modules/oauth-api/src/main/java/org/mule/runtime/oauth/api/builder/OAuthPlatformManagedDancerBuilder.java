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
   * Sets the URI that identifies the connection that is defined in the Anypoint Platform
   *
   * @param connectionUri the id of the connection which token we want to obtain
   * @return {@code this} builder
   */
  OAuthPlatformManagedDancerBuilder connectionUri(String connectionUri);

  /**
   * Sets the ID of the organization that defined the connection in the Anypoint Platform
   *
   * @param organizationId an organizationId
   * @return {@code this} builder
   */
  OAuthPlatformManagedDancerBuilder organizationId(String organizationId);

  /**
   * Sets the url of the platform API that serves the managed tokens
   *
   * @param platformUrl the url of the platform API that serves the managed tokens
   * @return {@code this} builder
   */
  OAuthPlatformManagedDancerBuilder platformUrl(String platformUrl);

  /**
   * Sets the environment identifier that needs to be connected to.
   *
   * @param environmentId an environment Id
   * @return {@code this} builder
   */
  OAuthPlatformManagedDancerBuilder environmentId(String environmentId);

  /**
   * Adds the {@code listener}. Listeners will be invoked in the same order as they were added
   *
   * @param listener the {@link PlatformManagedOAuthStateListener} to be added
   * @throws IllegalArgumentException if the {@code listener} is {@code null}
   */
  OAuthPlatformManagedDancerBuilder addListener(PlatformManagedOAuthStateListener listener);
}
