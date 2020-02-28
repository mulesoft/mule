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
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.connection.util.ConnectionProviderUtils.unwrapProviderWrapper;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractOfType;
import static org.mule.runtime.extension.api.security.CredentialsPlacement.BASIC_AUTH_HEADER;
import static org.mule.runtime.extension.api.security.CredentialsPlacement.BODY;
import static org.mule.runtime.extension.api.security.CredentialsPlacement.QUERY_PARAMS;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFields;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthCallbackValue;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantTypeVisitor;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;
import org.mule.runtime.extension.api.connectivity.oauth.PlatformManagedOAuthGrantType;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.security.CredentialsPlacement;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.loader.java.property.oauth.OAuthCallbackValuesModelProperty;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeConnectionProviderWrapper;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.ImmutableAuthorizationCodeState;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.runtime.oauth.api.builder.ClientCredentialsLocation;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

  /**
   * Traverses the {@code providerModel} looking for the Java fields annotated with {@link OAuthCallbackValue}. Then returns
   * a {@link Map} in which the keys are the {@link Field} objects and the values are the expressions to be evaluated to obtain
   * the values
   *
   * @param providerModel an OAuth enabled {@link ConnectionProviderModel}
   * @return a {@link Map} with the value with fields and expressions
   * @since 4.3.0
   */
  public static Map<Field, String> getCallbackValuesExtractors(ConnectionProviderModel providerModel) {
    return providerModel.getModelProperty(OAuthCallbackValuesModelProperty.class)
        .map(OAuthCallbackValuesModelProperty::getCallbackValues)
        .orElseGet(Collections::emptyMap);
  }

  /**
   * Returns a {@link FieldSetter} capable of setting a {@link OAuthState} field on the given {@code delegate}.
   *
   * @param connectionProvider the {@link ConnectionProvider} in which the value is to be set
   * @param stateType          the field's exact {@link OAuthState} type
   * @param grantType          the {@link OAuthGrantType} associated to the {@code stateType}
   * @param <C>                the generic type of the connections generated by the {@code delegate}
   * @param <T>                the generic type of the {@link OAuthState}
   * @retur a {@link FieldSetter} for the given {@code delegate}
   * @since 4.3.0
   */
  public static <C, T extends OAuthState> FieldSetter<ConnectionProvider<C>, T> getOAuthStateSetter(
                                                                                                    ConnectionProvider<C> connectionProvider,
                                                                                                    Class<T> stateType,
                                                                                                    OAuthGrantType grantType) {

    List<Field> stateFields = getFields(connectionProvider.getClass()).stream()
        .filter(f -> f.getType().equals(stateType))
        .collect(toList());

    if (stateFields.size() != 1) {
      throw new IllegalConnectionProviderModelDefinitionException(
                                                                  format("Connection Provider of class '%s' uses OAuth2 %s grant type and thus should contain "
                                                                      + "one (and only one) field of type %s. %d were found",
                                                                         connectionProvider.getClass().getName(),
                                                                         grantType.getName(),
                                                                         stateType,
                                                                         stateFields.size()));
    }

    return new FieldSetter<>(stateFields.get(0));
  }

  /**
   * Updates the {@code connectionProvider} state with the values from the given {@code context}. This includes not only
   * the provider's inner {@link OAuthState} but also the fields annotated with {@link OAuthCallbackValue}
   *
   * @param connectionProvider an OAuth enabled {@link ConnectionProvider}
   * @param callbackValues     a {@link Map} with {@link OAuthCallbackValue} annotated {@link Field fields } and the expressions
   *                           to be evaluated for each.
   * @param context            a {@link ResourceOwnerOAuthContext}
   * @param <C>                the generic type of the connections generated by the {@code connectionProvider}
   * @since 4.3.0
   */
  public static <C> void updateOAuthParameters(ConnectionProvider<C> connectionProvider,
                                               Map<Field, String> callbackValues,
                                               ResourceOwnerOAuthContext context) {
    Map<String, Object> responseParameters = context.getTokenResponseParameters();
    callbackValues.keySet().forEach(field -> {
      String key = field.getName();
      if (responseParameters.containsKey(key)) {
        new FieldSetter<>(field).set(connectionProvider, responseParameters.get(key));
      }
    });
  }

  /**
   * Invokes {@link ConnectionProvider#validate(Object)} on the {@code connectionProvider} only if the given {@code context}
   * returns a non {@code null} value for the {@link ResourceOwnerOAuthContext#getAccessToken()} method. A failure result is
   * returned otherwise
   *
   * @param connectionProvider a {@link ConnectionProvider}
   * @param connection         a connection object
   * @param context            a {@link ResourceOwnerOAuthContext}
   * @param <C>                the connection's generic type
   * @return a {@link ConnectionValidationResult}
   * @since 4.3.0
   */
  public static <C> ConnectionValidationResult validateOAuthConnection(ConnectionProvider<C> connectionProvider,
                                                                       C connection,
                                                                       ResourceOwnerOAuthContext context) {
    try {
      if (context.getAccessToken() != null) {
        return connectionProvider.validate(connection);
      } else {
        String message = "Server did not granted an access token";
        return failure(message, new IllegalStateException(message));
      }
    } catch (Exception e) {
      return failure("Could not obtain an access token", e);
    }
  }

  public static boolean refreshTokenIfNecessary(ExecutionContextAdapter<OperationModel> operationContext, Throwable e) {
    AccessTokenExpiredException expiredException = getTokenExpirationException(e);
    if (expiredException == null) {
      return false;
    }

    OAuthConnectionProviderWrapper connectionProvider = getOAuthConnectionProvider(operationContext);
    if (connectionProvider == null) {
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
        logTokenExpiration(operationContext);
      }

      @Override
      public void visit(PlatformManagedOAuthGrantType grantType) {
        logTokenExpiration(operationContext);
      }

      private void logTokenExpiration(ExecutionContextAdapter<OperationModel> operationContext) {
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

  private static AccessTokenExpiredException getTokenExpirationException(Throwable e) {
    return extractOfType(e, AccessTokenExpiredException.class).orElse(null);
  }

  private static String forResourceOwner(Optional<String> resourceOwnerId) {
    return resourceOwnerId.map(id -> "for resource owner '" + id + "'").orElse("");
  }

  private ExtensionsOAuthUtils() {}
}
