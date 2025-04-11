/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.api.annotation.NoImplement;
import org.mule.oauth.client.api.listener.ClientCredentialsListener;

import java.util.Map;

/**
 * Provides compatibility with version 1.x of the mule-oauth-client, which is a transitive api of the service api.
 *
 * @deprecated since 1.5, use {@link org.mule.oauth.client.api.builder.OAuthClientCredentialsDancerBuilder} from
 *             {@code mule-oauth-client 2.x}.
 */
@Deprecated
@NoImplement
public interface OAuthClientCredentialsDancerBuilder
    extends org.mule.oauth.client.api.builder.OAuthClientCredentialsDancerBuilder,
    org.mule.runtime.oauth.api.builder.OAuthDancerBuilder<org.mule.oauth.client.api.ClientCredentialsOAuthDancer> {

  @Override
  OAuthClientCredentialsDancerBuilder customParameters(Map<String, String> customParameters);

  @Override
  OAuthClientCredentialsDancerBuilder customHeaders(Map<String, String> customHeaders);

  @Override
  OAuthClientCredentialsDancerBuilder addListener(ClientCredentialsListener listener);

  OAuthClientCredentialsDancerBuilder addListener(org.mule.runtime.oauth.api.listener.ClientCredentialsListener listener);

  @Override
  @Deprecated
  OAuthClientCredentialsDancerBuilder encodeClientCredentialsInBody(boolean encodeClientCredentialsInBody);

}
