/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.oauth.api.ClientCredentialsOAuthDancer;
import org.mule.runtime.oauth.api.listener.ClientCredentialsListener;

import java.util.Map;

/**
 * Builder that allows to configure the attributes for the client credentials grant type.
 *
 * @since 4.0
 */
@NoImplement
public interface OAuthClientCredentialsDancerBuilder extends OAuthDancerBuilder<ClientCredentialsOAuthDancer> {

  OAuthClientCredentialsDancerBuilder customParameters(Map<String, String> customParameters);

  OAuthClientCredentialsDancerBuilder customHeaders(Map<String, String> customHeaders);

  OAuthClientCredentialsDancerBuilder customFormParameters(Map<String, String> customFormParameters);

  /**
   * Adds the {@code listener}. Listeners will be invoked in the same order as they were added
   *
   * @param listener the {@link ClientCredentialsListener} to be added
   * @throws IllegalArgumentException if the {@code listener} is {@code null}
   * @since 4.2.1
   */
  OAuthClientCredentialsDancerBuilder addListener(ClientCredentialsListener listener);

  /**
   * @param encodeClientCredentialsInBody If @{code true}, the client id and client secret will be sent in the request body.
   *                                      Otherwise, they will be sent as basic authentication.
   * @return this builder
   * @deprecated since 4.2.0. Use {@link OAuthClientCredentialsDancerBuilder#withClientCredentialsIn(ClientCredentialsLocation)} instead.
   */
  @Deprecated
  OAuthClientCredentialsDancerBuilder encodeClientCredentialsInBody(boolean encodeClientCredentialsInBody);

}
