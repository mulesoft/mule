/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.util.Preconditions;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Container for the possible configuration parameters for a {@code client-credentials} grant type.
 * 
 * @since 4.0
 */
public final class ClientCredentialsConfig extends AbstractOAuthConfig {

  private final boolean encodeClientCredentialsInBody;

  /**
   * Initializes a builder for a new config, starting with the mandatory parameters.
   * 
   * @param clientId
   * @param clientSecret
   * @param tokenUrl The OAuth authentication server url to get access to the token.
   * @return a new config builder with the mandatory paramters already set.
   */
  public static ClientCredentialsConfigBuilder builder(String clientId, String clientSecret, String tokenUrl) {
    ClientCredentialsConfigBuilder configBuilder = new ClientCredentialsConfigBuilder();
    configBuilder.clientId(clientId);
    configBuilder.clientSecret(clientSecret);
    configBuilder.tokenUrl(tokenUrl);
    return configBuilder;
  }

  /**
   * @return If {@code true}, the {@link #getClientId() client id} and {@link #getClientSecret() client secret} will be sent in
   *         the request body. Otherwise, they will be sent as basic authentication.
   */
  public boolean isEncodeClientCredentialsInBody() {
    return encodeClientCredentialsInBody;
  }

  private ClientCredentialsConfig(String clientId, String clientSecret, String tokenUrl, TlsContextFactory tlsContextFactory,
                                  String scopes, boolean encodeClientCredentialsInBody, Charset encoding,
                                  String responseAccessTokenExpr,
                                  String responseRefreshTokenExpr, String responseExpiresInExpr,
                                  Map<String, String> customParamtersExprs) {
    super(clientId, clientSecret, tokenUrl, tlsContextFactory, encoding, responseAccessTokenExpr, responseRefreshTokenExpr,
          responseExpiresInExpr, scopes, customParamtersExprs);
    this.encodeClientCredentialsInBody = encodeClientCredentialsInBody;
  }

  public static final class ClientCredentialsConfigBuilder extends AbstractOAuthConfigBuilder<ClientCredentialsConfig> {

    private boolean encodeClientCredentialsInBody = false;

    /**
     * @param encodeClientCredentialsInBody If @{code true}, the client id and client secret will be sent in the request body.
     *        Otherwise, they will be sent as basic authentication.
     */
    public ClientCredentialsConfigBuilder encodeClientCredentialsInBody(boolean encodeClientCredentialsInBody) {
      this.encodeClientCredentialsInBody = encodeClientCredentialsInBody;
      return this;
    }

    @Override
    public ClientCredentialsConfig build() {
      Preconditions.checkArgument(isNotBlank(clientId), "clientId cannot be blank");
      Preconditions.checkArgument(isNotBlank(clientSecret), "clientSecret cannot be blank");

      return new ClientCredentialsConfig(clientId, clientSecret, tokenUrl, tlsContextFactory, scopes,
                                         encodeClientCredentialsInBody, encoding, responseAccessTokenExpr,
                                         responseRefreshTokenExpr, responseExpiresInExpr, customParametersExtractorsExprs);
    }
  }

}
