/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.util.Preconditions;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.HttpServerConfiguration;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Container for the possible configuration parameters for a {@code authorization-code} grant type.
 * 
 * @since 4.0
 */
public final class AuthorizationCodeOAuthConfig extends AbstractOAuthConfig {

  // private HttpServer httpServer;
  // private Supplier<RequestHandlerManager> localCallbackRequestHandlerManagerFactory;
  // private TlsContextFactory tlsContextFactory;
  // private URL localCallbackUrl;
  //
  // private HttpServer httpServer;
  // private String localCallbackConfigPath;
  private BiFunction<OAuthHttpListenersServersManager, Supplier<Scheduler>, HttpServer> localCallbackServerFactory;
  private String localCallbackUrlPath;

  private String localAuthorizationUrlPath;
  private String localAuthorizationUrlResourceOwnerId;

  private String externalCallbackUrl;

  private String state;
  private String authorizationUrl;
  private Map<String, String> customParameters;

  /**
   * Initializes a builder for a new config, starting with the mandatory parameters.
   * 
   * @param clientId
   * @param clientSecret
   * @param tokenUrl The OAuth authentication server url to get access to the token.
   * @return a new config builder with the mandatory parameters already set.
   */
  public static AuthorizationCodeOAuthConfigBuilder builder(String clientId, String clientSecret, String tokenUrl) {
    AuthorizationCodeOAuthConfigBuilder configBuilder = new AuthorizationCodeOAuthConfigBuilder();
    configBuilder.clientId(clientId);
    configBuilder.clientSecret(clientSecret);
    configBuilder.tokenUrl(tokenUrl);
    return configBuilder;
  }

  public HttpServer getHttpServer(OAuthHttpListenersServersManager serversManager,
                                  Supplier<Scheduler> schedulerFactory) {
    return localCallbackServerFactory.apply(serversManager, schedulerFactory);
  }

  public String getLocalCallbackUrlPath() {
    return localCallbackUrlPath;
  }

  /**
   * If this attribute is provided mule will automatically create and endpoint in the host server that the user can hit to
   * authenticate and grant access to the application for his account.
   * 
   * @return the path to listen for the callback
   */
  public String getLocalAuthorizationUrlPath() {
    return localAuthorizationUrlPath;
  }

  /**
   * This attribute is only required when the applications needs to access resources from more than one user in the OAuth
   * authentication server.
   * 
   * @return identifier under which the oauth authentication attributes are stored (accessToken, refreshToken, etc).
   */
  public String getLocalAuthorizationUrlResourceOwnerId() {
    return localAuthorizationUrlResourceOwnerId;
  }

  /**
   * The oauth authentication server will use this url to provide the authentication code to the Mule server so the mule server
   * can retrieve the access token.
   * <p>
   * Note that this must be the externally visible address of the callback, not the local one.
   * 
   * @return the callback url where the authorization code will be received.
   */
  public String getExternalCallbackUrl() {
    return externalCallbackUrl;
  }

  /**
   * @return parameter for holding state between the authentication request and the callback done by the OAuth authorization
   *         server to the {@code redirectUrl}.
   */
  public String getState() {
    return state;
  }

  /**
   * @return The OAuth authentication server url to authorize the app for a certain user.
   */
  public String getAuthorizationUrl() {
    return authorizationUrl;
  }

  /**
   * @return custom parameters to send to the authorization request url or the oauth authorization sever.
   */
  public Map<String, String> getCustomParameters() {
    return customParameters;
  }

  private AuthorizationCodeOAuthConfig(String clientId, String clientSecret, String tokenUrl, TlsContextFactory tlsContextFactory,
                                       String scopes, String externalCallbackUrl, Charset encoding,
                                       BiFunction<OAuthHttpListenersServersManager, Supplier<Scheduler>, HttpServer> localCallbackServerFactory,
                                       String localCallbackUrlPath, String localAuthorizationUrlPath,
                                       String localAuthorizationUrlResourceOwnerId, String state, String authorizationUrl,
                                       String responseAccessTokenExpr, String responseRefreshTokenExpr,
                                       String responseExpiresInExpr, Map<String, String> customParameters,
                                       Map<String, String> customParametersExtractorsExprs) {
    super(clientId, clientSecret, tokenUrl, tlsContextFactory, encoding, responseAccessTokenExpr, responseRefreshTokenExpr,
          responseExpiresInExpr,
          scopes, customParametersExtractorsExprs);
    this.localCallbackServerFactory = localCallbackServerFactory;
    this.localCallbackUrlPath = localCallbackUrlPath;
    this.localAuthorizationUrlPath = localAuthorizationUrlPath;
    this.localAuthorizationUrlResourceOwnerId = localAuthorizationUrlResourceOwnerId;
    this.state = state;
    this.authorizationUrl = authorizationUrl;
    this.externalCallbackUrl = externalCallbackUrl;
    this.customParameters = customParameters;
  }

  public static final class AuthorizationCodeOAuthConfigBuilder extends AbstractOAuthConfigBuilder<AuthorizationCodeOAuthConfig> {

    private BiFunction<OAuthHttpListenersServersManager, Supplier<Scheduler>, HttpServer> localCallbackServerFactory;
    private String localCallbackUrlPath;
    private String localAuthorizationUrlPath;
    private String localAuthorizationUrlResourceOwnerId;
    private String externalCallbackUrl;

    private String state;
    private String authorizationUrl;

    private Map<String, String> customParameters;

    public AuthorizationCodeOAuthConfigBuilder localCallback(URL localCallbackUrl,
                                                             Optional<TlsContextFactory> tlsContextFactory) {
      localCallbackServerFactory = (serversManager, schedulerFactory) -> {
        final HttpServerConfiguration.Builder serverConfigBuilder = new HttpServerConfiguration.Builder();
        serverConfigBuilder.setHost(localCallbackUrl.getHost()).setPort(localCallbackUrl.getPort());
        tlsContextFactory.ifPresent(tls -> serverConfigBuilder.setTlsContextFactory(tls));
        serverConfigBuilder.setSchedulerSupplier(schedulerFactory);
        try {
          return serversManager.getServer(serverConfigBuilder.build());
        } catch (ConnectionException e) {
          throw new MuleRuntimeException(e);
        }
      };
      localCallbackUrlPath = localCallbackUrl.getPath();

      return this;
    }

    public AuthorizationCodeOAuthConfigBuilder localCallback(HttpServer httpServer, String localCallbackConfigPath) {
      localCallbackServerFactory = (serversManager, schedulerFactory) -> httpServer;
      localCallbackUrlPath = localCallbackConfigPath;
      return this;
    }

    /**
     * If this attribute is provided mule will automatically create and endpoint in the host server that the user can hit to
     * authenticate and grant access to the application for his account.
     * 
     * @param localAuthorizationUrlPath the path to listen for the callback
     */
    public AuthorizationCodeOAuthConfigBuilder localAuthorizationUrlPath(String localAuthorizationUrlPath) {
      this.localAuthorizationUrlPath = localAuthorizationUrlPath;
      return this;
    }

    /**
     * This attribute is only required when the applications needs to access resources from more than one user in the OAuth
     * authentication server.
     * 
     * @param localAuthorizationUrlResourceOwnerId identifier under which the oauth authentication attributes are stored
     *        (accessToken, refreshToken, etc).
     */
    public AuthorizationCodeOAuthConfigBuilder localAuthorizationUrlResourceOwnerId(String localAuthorizationUrlResourceOwnerId) {
      this.localAuthorizationUrlResourceOwnerId = localAuthorizationUrlResourceOwnerId;
      return this;
    }

    /**
     * The oauth authentication server will use this url to provide the authentication code to the Mule server so the mule server
     * can retrieve the access token.
     * <p>
     * Note that this must be the externally visible address of the callback, not the local one.
     * 
     * @param externalCallbackUrl the callback url where the authorization code will be received.
     */
    public AuthorizationCodeOAuthConfigBuilder externalCallbackUrl(String externalCallbackUrl) {
      this.externalCallbackUrl = externalCallbackUrl;
      return this;
    }

    /**
     * @param state parameter for holding state between the authentication request and the callback done by the OAuth
     *        authorization server to the {@code redirectUrl}.
     */
    public AuthorizationCodeOAuthConfigBuilder state(String state) {
      this.state = state;
      return this;
    }

    /**
     * @param authorizationUrl The OAuth authentication server url to authorize the app for a certain user.
     */
    public AuthorizationCodeOAuthConfigBuilder authorizationUrl(String authorizationUrl) {
      this.authorizationUrl = authorizationUrl;
      return this;
    }

    /**
     * @param customParameters
     */
    public AuthorizationCodeOAuthConfigBuilder customParameters(Map<String, String> customParameters) {
      this.customParameters = customParameters;
      return this;
    }

    @Override
    public AuthorizationCodeOAuthConfig build() {
      Preconditions.checkArgument(isNotBlank(clientId), "clientId cannot be blank");
      Preconditions.checkArgument(isNotBlank(clientSecret), "clientSecret cannot be blank");
      Preconditions.checkArgument(isNotBlank(authorizationUrl), "authorizationUrl cannot be blank");
      Preconditions.checkArgument(customParameters != null, "customParameters cannot be null");

      return new AuthorizationCodeOAuthConfig(clientId, clientSecret, tokenUrl, tlsContextFactory, scopes, externalCallbackUrl,
                                              encoding, localCallbackServerFactory, localCallbackUrlPath,
                                              localAuthorizationUrlPath, localAuthorizationUrlResourceOwnerId, state,
                                              authorizationUrl, responseAccessTokenExpr, responseRefreshTokenExpr,
                                              responseExpiresInExpr, customParameters, customParametersExtractorsExprs);
    }
  }

}
