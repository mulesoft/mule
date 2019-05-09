/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.runtime.oauth.api.ClientCredentialsOAuthDancer;

/**
 * Builder that allows to configure the attributes for the client credentials grant type.
 *
 * @since 4.0
 */
public interface OAuthClientCredentialsDancerBuilder extends OAuthDancerBuilder<ClientCredentialsOAuthDancer> {

  /**
   * Adds the {@code listener}. Listeners will be invoked in the same order as they were added
   *
   * @param listener the {@link ClientCredentialsListener} to be added
   * @throws IllegalArgumentException if the {@code listener} is {@code null}
   * @since 4.2.1
   */
  void addListener(ClientCredentialsListener listener);

  /**
   * @param encodeClientCredentialsInBody If @{code true}, the client id and client secret will be sent in the request body.
   *        Otherwise, they will be sent as basic authentication.
   *
   * @deprecated since 4.2.0. Use {@link OAuthClientCredentialsDancerBuilder#withClientCredentialsIn(ClientCredentialsLocation)} instead.
   * 
   * @return this builder
   */
  @Deprecated
  OAuthClientCredentialsDancerBuilder encodeClientCredentialsInBody(boolean encodeClientCredentialsInBody);

}
