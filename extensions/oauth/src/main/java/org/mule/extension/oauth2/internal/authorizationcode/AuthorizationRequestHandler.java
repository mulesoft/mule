/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode;

import static java.util.Collections.singletonMap;
import static org.mule.extension.oauth2.internal.authorizationcode.RequestHandlerUtils.addRequestHandler;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.service.http.api.HttpConstants.HttpStatus.MOVED_TEMPORARILY;
import static org.mule.service.http.api.HttpConstants.Method.GET;
import static org.mule.service.http.api.HttpHeaders.Names.LOCATION;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.oauth2.internal.DeferredExpressionResolver;
import org.mule.extension.oauth2.internal.StateEncoder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.server.RequestHandlerManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;

/**
 * Handles the call to the {@code localAuthorizationUrl} and redirects the user to the oauth authentication server authorization
 * url so the user can grant access to the resources to the mule application.
 */
// TODO MULE-11412 Remove MuleContextAware
public class AuthorizationRequestHandler implements MuleContextAware, Startable, Stoppable {

  private static final Logger LOGGER = getLogger(AuthorizationRequestHandler.class);

  public static final String OAUTH_STATE_ID_FLOW_VAR_NAME = "resourceOwnerId";

  /**
   * Scope required by this application to execute. Scopes define permissions over resources.
   */
  @Parameter
  @Optional
  private String scopes;

  /**
   * State parameter for holding state between the authentication request and the callback done by the oauth authorization server
   * to the redirectUrl.
   */
  @Parameter
  @Optional
  private ParameterResolver<String> state;

  /**
   * Identifier under which the oauth authentication attributes are stored (accessToken, refreshToken, etc).
   * <p>
   * This attribute is only required when the applications needs to access resources from more than one user in the OAuth
   * authentication server.
   */
  @Parameter
  @Optional
  private ParameterResolver<String> localAuthorizationUrlResourceOwnerId;

  /**
   * If this attribute is provided mule will automatically create and endpoint in the host server that the user can hit to
   * authenticate and grant access to the application for his account.
   */
  @Parameter
  private String localAuthorizationUrl;

  /**
   * The oauth authentication server url to authorize the app for a certain user.
   */
  @Parameter
  private String authorizationUrl;

  /**
   * Custom parameters to send to the authorization request url or the oauth authorization sever.
   */
  @Parameter
  @Optional
  @Alias("custom-parameters")
  private Map<String, String> customParameters = new HashMap<>();

  private RequestHandlerManager redirectUrlHandlerManager;
  // TODO MULE-11412 Uncomment
  // @Inject
  private MuleContext muleContext;
  private DeferredExpressionResolver resolver;
  private AuthorizationCodeGrantType oauthConfig;

  public void init() throws MuleException {
    try {
      this.redirectUrlHandlerManager =
          addRequestHandler(getOauthConfig().getServer(),
                            GET, new URL(localAuthorizationUrl).getPath(),
                            getDefaultEncoding(muleContext),
                            createLocalAuthorizationUrlListener(),
                            LOGGER);
    } catch (MalformedURLException e) {
      LOGGER.warn("Could not parse provided url %s. Validate that the url is correct", localAuthorizationUrl);
      throw new DefaultMuleException(e);
    }
  }

  private Function<Result<Object, HttpRequestAttributes>, Result<Object, HttpResponseAttributes>> createLocalAuthorizationUrlListener() {
    return in -> {
      final String onCompleteRedirectToValue = in.getAttributes().get().getQueryParams().get("onCompleteRedirectTo");
      final String resourceOwnerId = resolver.resolveExpression(localAuthorizationUrlResourceOwnerId, in);

      final StateEncoder stateEncoder = new StateEncoder(resolver.resolveExpression(state, in));
      if (resourceOwnerId != null) {
        stateEncoder.encodeResourceOwnerIdInState(resourceOwnerId);
      }
      if (onCompleteRedirectToValue != null) {
        stateEncoder.encodeOnCompleteRedirectToInState(onCompleteRedirectToValue);
      }
      final String authorizationUrlWithParams = new AuthorizationRequestUrlBuilder().setAuthorizationUrl(authorizationUrl)
          .setClientId(oauthConfig.getClientId()).setClientSecret(oauthConfig.getClientSecret())
          .setCustomParameters(customParameters).setRedirectUrl(oauthConfig.getExternalCallbackUrl())
          .setState(stateEncoder.getEncodedState()).setScope(scopes).buildUrl();

      return Result.<Object, HttpResponseAttributes>builder().output(in.getOutput()).mediaType(in.getMediaType().get())
          .attributes(new HttpResponseAttributes(MOVED_TEMPORARILY.getStatusCode(), MOVED_TEMPORARILY.getReasonPhrase(),
                                                 new ParameterMap(singletonMap(LOCATION, authorizationUrlWithParams))))
          .build();
    };
  }

  @Override
  public void setMuleContext(final MuleContext muleContext) {
    this.muleContext = muleContext;
    this.resolver = new DeferredExpressionResolver(muleContext);
  }

  public void setOauthConfig(final AuthorizationCodeGrantType oauthConfig) {
    this.oauthConfig = oauthConfig;
  }

  public AuthorizationCodeGrantType getOauthConfig() {
    return oauthConfig;
  }

  @Override
  public void start() {
    redirectUrlHandlerManager.start();
  }

  @Override
  public void stop() {
    redirectUrlHandlerManager.stop();
  }

}
