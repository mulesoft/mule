/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.tls.TlsContextFactory;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

public class AbstractOAuthConfig {

  private final String clientId;
  private final String clientSecret;
  private final String tokenUrl;
  private final TlsContextFactory tlsContextFactory;

  private final Charset encoding;
  private final String responseAccessTokenExpr;
  private final String responseRefreshTokenExpr;
  private final String responseExpiresInExpr;
  private final String scopes;
  private final Map<String, String> customParametersExtractorsExprs;

  /**
   * @return the application identifier as defined in the OAuth authentication server.
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * @return the application secret as defined in the OAuth authentication server.
   */
  public String getClientSecret() {
    return clientSecret;
  }

  /**
   * Mule, after receiving the authentication code from the OAuth server (through the {@code redirectUrl}) will call this url to
   * get the access token.
   * 
   * @return The OAuth authentication server url to get access to the token.
   */
  public String getTokenUrl() {
    return tokenUrl;
  }

  /**
   * @return a TLS config that will be used to receive incoming HTTP request and do HTTP request during the OAuth dance.
   */
  public Optional<TlsContextFactory> getTlsContextFactory() {
    return ofNullable(tlsContextFactory);
  }

  public Charset getEncoding() {
    return encoding;
  }

  /**
   * @return an expression to extract the access token parameter from the response of the call to {@link #getTokenUrl()
   *         token-url}.
   */
  public String getResponseAccessTokenExpr() {
    return responseAccessTokenExpr;
  }

  public String getResponseRefreshTokenExpr() {
    return responseRefreshTokenExpr;
  }

  /**
   * @return an expression to extract the {@code expiresIn} parameter from the response of the call to {@link #getTokenUrl()
   *         token-url}.
   */
  public String getResponseExpiresInExpr() {
    return responseExpiresInExpr;
  }

  /**
   * Scopes define permissions over resources.
   * 
   * @return Scope required by this application to execute
   */
  public String getScopes() {
    return scopes;
  }

  /**
   * @return customParamtersExprs custom parameters to send to the authorization request url or the oauth authorization sever.
   */
  public Map<String, String> getCustomParametersExtractorsExprs() {
    return customParametersExtractorsExprs;
  }

  protected AbstractOAuthConfig(String clientId, String clientSecret, String tokenUrl, TlsContextFactory tlsContextFactory,
                                Charset encoding, String responseAccessTokenExpr, String responseRefreshTokenExpr,
                                String responseExpiresInExpr, String scopes,
                                Map<String, String> customParametersExtractorsExprs) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.tokenUrl = tokenUrl;
    this.tlsContextFactory = tlsContextFactory;

    this.encoding = encoding;
    this.responseAccessTokenExpr = responseAccessTokenExpr;
    this.responseRefreshTokenExpr = responseRefreshTokenExpr;
    this.responseExpiresInExpr = responseExpiresInExpr;
    this.scopes = scopes;
    this.customParametersExtractorsExprs = customParametersExtractorsExprs;
  }

  public static abstract class AbstractOAuthConfigBuilder<C extends AbstractOAuthConfig> {

    protected String clientId;
    protected String clientSecret;
    protected String tokenUrl;
    protected TlsContextFactory tlsContextFactory;

    protected Charset encoding;
    protected String responseAccessTokenExpr;
    protected String responseRefreshTokenExpr;
    protected String responseExpiresInExpr;
    protected String scopes = null;
    protected Map<String, String> customParametersExtractorsExprs;

    /**
     * @param clientId the application identifier as defined in the OAuth authentication server.
     */
    public AbstractOAuthConfigBuilder<C> clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    /**
     * @param clientSecret the application secret as defined in the OAuth authentication server.
     */
    public AbstractOAuthConfigBuilder<C> clientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
      return this;
    }

    /**
     * Mule, after receiving the authentication code from the OAuth server (through the redirectUrl) will call this url to get the
     * access token.
     * 
     * @param tokenUrl The OAuth authentication server url to get access to the token.
     */
    public AbstractOAuthConfigBuilder<C> tokenUrl(String tokenUrl) {
      this.tokenUrl = tokenUrl;
      return this;
    }

    /**
     * @param tlsContextFactory References a TLS config that will be used to receive incoming HTTP request and do HTTP request
     *        during the OAuth dance.
     */
    public AbstractOAuthConfigBuilder<C> tlsContextFactory(TlsContextFactory tlsContextFactory) {
      this.tlsContextFactory = tlsContextFactory;
      return this;
    }

    public AbstractOAuthConfigBuilder<C> encoding(Charset encoding) {
      this.encoding = encoding;
      return this;
    }

    /**
     * @param responseAccessTokenExpr an expression to extract the {@code access token} parameter from the response of the call to
     *        {@link #getTokenUrl() token-url}.
     */
    public AbstractOAuthConfigBuilder<C> responseAccessTokenExpr(String responseAccessTokenExpr) {
      this.responseAccessTokenExpr = responseAccessTokenExpr;
      return this;
    }

    public AbstractOAuthConfigBuilder<C> responseRefreshTokenExpr(String responseRefreshTokenExpr) {
      this.responseRefreshTokenExpr = responseRefreshTokenExpr;
      return this;
    }

    /**
     * @param responseExpiresInExpr an expression to extract the {@code expiresIn} parameter from the response of the call to
     *        {@link #getTokenUrl() token-url}.
     */
    public AbstractOAuthConfigBuilder<C> responseExpiresInExpr(String responseExpiresInExpr) {
      this.responseExpiresInExpr = responseExpiresInExpr;
      return this;
    }

    /**
     * Scopes define permissions over resources.
     * 
     * @param scopes required by this application to execute.
     */
    public AbstractOAuthConfigBuilder<C> scopes(String scopes) {
      this.scopes = scopes;
      return this;
    }

    public AbstractOAuthConfigBuilder<C> customParametersExtractorsExprs(Map<String, String> customParametersExtractorsExprs) {
      this.customParametersExtractorsExprs = customParametersExtractorsExprs;
      return this;
    }

    /**
     * @return a new configuration object that may be used to create a {@link OAuthDancer dancer}.
     */
    public abstract C build();
  }
}
