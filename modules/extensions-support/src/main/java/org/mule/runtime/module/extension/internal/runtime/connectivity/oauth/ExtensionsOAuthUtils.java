/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.connection.util.ConnectionProviderUtils.unwrapProviderWrapper;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractOfType;
import static org.mule.runtime.extension.api.security.CredentialsPlacement.BASIC_AUTH_HEADER;
import static org.mule.runtime.extension.api.security.CredentialsPlacement.BODY;
import static org.mule.runtime.extension.api.security.CredentialsPlacement.QUERY_PARAMS;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantTypeVisitor;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.security.CredentialsPlacement;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeConnectionProviderWrapper;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.ImmutableAuthorizationCodeState;
import org.mule.runtime.oauth.api.builder.ClientCredentialsLocation;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.Optional;

import org.slf4j.Logger;

/**
 * Utility methods for the OAuth support on the SDK
 *
 * @since 4.0
 */
public final class ExtensionsOAuthUtils {

  private static final Logger LOGGER = getLogger(ExtensionsOAuthUtils.class);

  public static AuthorizationCodeState toAuthorizationCodeState(AuthorizationCodeConfig config,
                                                                ResourceOwnerOAuthContext context) {
    return new ImmutableAuthorizationCodeState(context.getAccessToken(),
                                               context.getRefreshToken(),
                                               context.getResourceOwnerId(),
                                               context.getExpiresIn(),
                                               context.getState(),
                                               config.getAuthorizationUrl(),
                                               config.getAccessTokenUrl(),
                                               config.getCallbackConfig().getExternalCallbackUrl(),
                                               config.getConsumerKey(),
                                               config.getConsumerSecret());
  }

  public static ClientCredentialsLocation toCredentialsLocation(CredentialsPlacement placement) {
    if (placement == BASIC_AUTH_HEADER) {
      return ClientCredentialsLocation.BASIC_AUTH_HEADER;
    } else if (placement == QUERY_PARAMS) {
      return ClientCredentialsLocation.QUERY_PARAMS;
    } else if (placement == BODY) {
      return ClientCredentialsLocation.BODY;
    } else {
      throw new IllegalArgumentException("Unsupported CredentialsPlacement type " + placement.name());
    }
  }

  public static OAuthConnectionProviderWrapper getOAuthConnectionProvider(ExecutionContextAdapter operationContext) {
    ConfigurationInstance config = ((ConfigurationInstance) operationContext.getConfiguration().get());
    ConnectionProvider provider =
        unwrapProviderWrapper(config.getConnectionProvider().get(), OAuthConnectionProviderWrapper.class);
    return provider instanceof OAuthConnectionProviderWrapper ? (OAuthConnectionProviderWrapper) provider : null;
  }

  public static boolean refreshTokenIfNecessary(ExecutionContextAdapter<OperationModel> operationContext, Exception e) {
    OAuthConnectionProviderWrapper connectionProvider = getOAuthConnectionProvider(operationContext);
    if (connectionProvider == null) {
      return false;
    }

    AccessTokenExpiredException expiredException = getTokenExpirationException(e);
    if (expiredException == null) {
      return false;
    }

    Reference<Optional<String>> resourceOwnerIdReference = new Reference<>(empty());
    connectionProvider.getGrantType().accept(new OAuthGrantTypeVisitor() {

      @Override
      public void visit(AuthorizationCodeGrantType grantType) {
        AuthorizationCodeConnectionProviderWrapper cp = (AuthorizationCodeConnectionProviderWrapper) connectionProvider;
        String rsId = cp.getResourceOwnerId();
        resourceOwnerIdReference.set(of(rsId));

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("AccessToken for resourceOwner '{}' expired at operation '{}:{}' using config '{}'. "
              + "Will attempt to refresh token and retry operation",
                       rsId, operationContext.getExtensionModel().getName(),
                       operationContext.getComponentModel().getName(),
                       operationContext.getConfiguration().get().getName());
        }
      }

      @Override
      public void visit(ClientCredentialsGrantType grantType) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("AccessToken expired at operation '{}:{}' using config '{}'. "
              + "Will attempt to refresh token and retry operation",
                       operationContext.getExtensionModel().getName(),
                       operationContext.getComponentModel().getName(),
                       operationContext.getConfiguration().get().getName());
        }
      }
    });

    Optional<String> resourceOwnerId = resourceOwnerIdReference.get();

    try {
      connectionProvider.refreshToken(resourceOwnerId.orElse(""));
    } catch (Exception refreshException) {
      throw new MuleRuntimeException(createStaticMessage(format(
                                                                "AccessToken %s expired at operation '%s:%s' using config '%s'. Refresh token "
                                                                    + "workflow was attempted but failed with the following exception",
                                                                forResourceOwner(resourceOwnerId),
                                                                operationContext.getExtensionModel().getName(),
                                                                operationContext.getComponentModel().getName(),
                                                                operationContext.getConfiguration().get().getName())),
                                     refreshException);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Access Token successfully refreshed {} on config '{}'",
                   forResourceOwner(resourceOwnerId), operationContext.getConfiguration().get().getName());
    }

    return true;
  }

  private static AccessTokenExpiredException getTokenExpirationException(Exception e) {
    return extractOfType(e, AccessTokenExpiredException.class).orElse(null);
  }

  private static String forResourceOwner(Optional<String> resourceOwnerId) {
    return resourceOwnerId.map(id -> "for resource owner '" + id + "'").orElse("");
  }

  private ExtensionsOAuthUtils() {}
}
