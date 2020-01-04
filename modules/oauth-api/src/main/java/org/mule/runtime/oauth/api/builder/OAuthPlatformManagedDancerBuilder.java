/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.proxy.ProxyConfig;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.listener.PlatformManagedListener;

import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * Builder that allows to configure the attributes for the {@link PlatformManagedOAuthDancer}
 * <p>
 * Because the actual OAuth authorization is not performed by Mule but the Anypoint Platform, some of the methods in the base
 * {@link OAuthDancerBuilder} do not apply and will throw an {@link UnsupportedOperationException}. The the javadocs of all
 * methods defined and overridden in this interface to know which ones are valid.
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

  /**
   * Adds the {@code listener}. Listeners will be invoked in the same order as they were added
   *
   * @param listener the {@link PlatformManagedListener} to be added
   * @throws IllegalArgumentException if the {@code listener} is {@code null}
   * @since 4.2.1
   */
  OAuthPlatformManagedDancerBuilder addListener(PlatformManagedListener listener);

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException
   */
  @Override
  default OAuthDancerBuilder tokenUrl(String tokenUrl) {
    throw new UnsupportedOperationException("This operation does not apply to this builder type");
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException
   */
  @Override
  default OAuthDancerBuilder tokenUrl(HttpClient httpClient, String tokenUrl) {
    throw new UnsupportedOperationException("This operation does not apply to this builder type");
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException
   */
  @Override
  default OAuthDancerBuilder tokenUrl(String tokenUrl, TlsContextFactory tlsContextFactory) {
    throw new UnsupportedOperationException("This operation does not apply to this builder type");
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException
   */
  @Override
  default OAuthDancerBuilder<PlatformManagedOAuthDancer> tokenUrl(String tokenUrl, ProxyConfig proxyConfig) {
    throw new UnsupportedOperationException("This operation does not apply to this builder type");
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException
   */
  @Override
  default OAuthDancerBuilder<PlatformManagedOAuthDancer> tokenUrl(String tokenUrl,
                                                                  TlsContextFactory tlsContextFactory,
                                                                  ProxyConfig proxyConfig) {
    throw new UnsupportedOperationException("This operation does not apply to this builder type");
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException
   */
  @Override
  default OAuthDancerBuilder encoding(Charset encoding) {
    throw new UnsupportedOperationException("This operation does not apply to this builder type");
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException
   */
  @Override
  default OAuthDancerBuilder clientCredentials(String clientId, String clientSecret) {
    throw new UnsupportedOperationException("This operation does not apply to this builder type");
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException
   */
  @Override
  default OAuthDancerBuilder<PlatformManagedOAuthDancer> resourceOwnerIdTransformer(
      Function<String, String> resourceOwnerIdTransformer) {
    throw new UnsupportedOperationException("This operation does not apply to this builder type");
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException
   */
  @Override
  default OAuthDancerBuilder scopes(String scopes) {
    throw new UnsupportedOperationException("This operation does not apply to this builder type");
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException
   */
  @Override
  default OAuthDancerBuilder withClientCredentialsIn(ClientCredentialsLocation clientCredentialsLocation) {
    throw new UnsupportedOperationException("This operation does not apply to this builder type");
  }
}
