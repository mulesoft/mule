/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

import org.mule.oauth.client.api.builder.ClientCredentialsLocation;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthCallbackValue;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;
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
import org.mule.sdk.api.connectivity.oauth.AccessTokenExpiredException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * Utility methods for the OAuth support on the SDK
 *
 * @since 4.0
 */
public final class ExtensionsOAuthUtils {

  public static final List<Class<?>> AUTHORIZATION_CODE_STATE_INTERFACES =
      Arrays.asList(AuthorizationCodeState.class, org.mule.sdk.api.connectivity.oauth.AuthorizationCodeState.class);
  public static final List<Class<?>> CLIENT_CREDENTIALS_STATE_INTERFACES =
      Arrays.asList(ClientCredentialsState.class, org.mule.sdk.api.connectivity.oauth.ClientCredentialsState.class);;

  private static final Logger LOGGER = getLogger(ExtensionsOAuthUtils.class);

  /**
   * Default max number of times that a refresh token and retry workflow should be executed for a component failing with
   * {@link AccessTokenExpiredException}
   */
  public static final int MAX_REFRESH_ATTEMPTS = 2;

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
    return getOAuthConnectionProvider(config.getConnectionProvider().get());
  }

  /**
   * Unwraps a given connection provider if necessary to get a {@link OAuthConnectionProviderWrapper}.
   *
   * @param provider connection provider to unwrap
   * @since 4.4.0
   * @return
   */
  public static OAuthConnectionProviderWrapper getOAuthConnectionProvider(ConnectionProvider provider) {
    ConnectionProvider oauthProvider = unwrapProviderWrapper(provider, OAuthConnectionProviderWrapper.class);
    return oauthProvider instanceof OAuthConnectionProviderWrapper ? (OAuthConnectionProviderWrapper) oauthProvider : null;
  }

  /**
   * Traverses the {@code providerModel} looking for the Java fields annotated with {@link OAuthCallbackValue}. Then returns a
   * {@link Map} in which the keys are the {@link Field} objects and the values are the expressions to be evaluated to obtain the
   * values
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
   * Returns a {@link FieldSetter} capable of setting a {@link OAuthState} field on the given {@code target}.
   *
   * @param target     the target in which the value is to be set
   * @param stateTypes the possible field's exact {@link OAuthState} type
   * @param grantType  the {@link OAuthGrantType} associated to the {@code stateType}
   * @param <T>        the generic type of the {@link OAuthState}
   * @retur a {@link FieldSetter} for the given {@code delegate}
   * @since 4.3.0
   */
  public static <T extends OAuthState> FieldSetter<Object, Object> getOAuthStateSetter(Object target,
                                                                                       List<Class<?>> stateTypes,
                                                                                       OAuthGrantType grantType) {

    List<Field> stateFields = getFields(target.getClass()).stream()
        .filter(f -> stateTypes.stream().anyMatch(stateType -> f.getType().equals(stateType)))
        .collect(toList());

    if (stateFields.size() != 1) {
      throw new IllegalConnectionProviderModelDefinitionException(
                                                                  format("Connection Provider of class '%s' uses OAuth2 %s grant type and thus should contain "
                                                                      + "one (and only one) field of type %s. %d were found",
                                                                         target.getClass().getName(),
                                                                         grantType.getName(),
                                                                         stateTypes.get(0).getName(),
                                                                         stateFields.size()));
    }

    return new FieldSetter<>(stateFields.get(0));
  }

  /**
   * Updates the {@code target} state with the values from the given {@code context}. This includes not only the provider's inner
   * {@link OAuthState} but also the fields annotated with {@link OAuthCallbackValue}
   *
   * @param target         an OAuth enabled target
   * @param callbackValues a {@link Map} with {@link OAuthCallbackValue} annotated {@link Field fields } and the expressions to be
   *                       evaluated for each.
   * @param context        a {@link ResourceOwnerOAuthContext}
   * @param <C>            the generic type of the connections generated by the {@code connectionProvider}
   * @since 4.3.0
   */
  public static <C> void updateOAuthParameters(Object target,
                                               Map<Field, String> callbackValues,
                                               ResourceOwnerOAuthContext context) {
    Map<String, Object> responseParameters = context.getTokenResponseParameters();
    callbackValues.keySet().forEach(field -> {
      String key = field.getName();
      if (responseParameters.containsKey(key)) {
        new FieldSetter<>(field).set(target, responseParameters.get(key));
      }
    });
  }

  /**
   * Invokes {@link ConnectionProvider#validate(Object)} on the {@code connectionProvider} only if the given {@code context}
   * returns a non {@code null} value for the {@link ResourceOwnerOAuthContext#getAccessToken()} method. A failure result is
   * returned otherwise
   *
   * @param oAuthConnectionProviderWrapper a {@link OAuthConnectionProviderWrapper}
   * @param connection                     a connection object
   * @param context                        a {@link ResourceOwnerOAuthContext}
   * @param <C>                            the connection's generic type
   * @return a {@link ConnectionValidationResult}
   * @since 4.3.0
   */
  public static <C> ConnectionValidationResult validateOAuthConnection(OAuthConnectionProviderWrapper<C> oAuthConnectionProviderWrapper,
                                                                       C connection,
                                                                       ResourceOwnerOAuthContext context) {
    return validateOAuthConnection(oAuthConnectionProviderWrapper, connection, context, MAX_REFRESH_ATTEMPTS);
  }

  private static <C> ConnectionValidationResult validateOAuthConnection(OAuthConnectionProviderWrapper<C> oAuthConnectionProviderWrapper,
                                                                        C connection,
                                                                        ResourceOwnerOAuthContext context,
                                                                        int maxRefreshAttempts) {
    ConnectionValidationResult connectionValidationResult =
        validateOAuthConnection(oAuthConnectionProviderWrapper.getDelegate(), connection, context);
    if (!connectionValidationResult.isValid() && maxRefreshAttempts > 0
        && refreshTokenIfNecessary(oAuthConnectionProviderWrapper, connectionValidationResult.getException())) {
      return validateOAuthConnection(oAuthConnectionProviderWrapper, connection, context, --maxRefreshAttempts);
    }
    return connectionValidationResult;
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

  /**
   * Gets the value provided by a {@link CheckedSupplier}, if that fails and the reason is that a token refresh is needed, the
   * refresh is performed up to {@link #MAX_REFRESH_ATTEMPTS} times and the supplier is prompted to provide the value again.
   *
   * @param connectionProviderSupplier a supplier of the connection provider that created a connection used in the given supplier
   *                                   to provide a value.
   * @param supplier                   a supplier that depends on an oauth based connection to provide a value.
   * @param <T>                        the type of the value to be provided
   * @return the value the supplier gives
   * @throws Exception
   * @since 4.4.0
   */
  public static <T> T withRefreshToken(Supplier<ConnectionProvider> connectionProviderSupplier, CheckedSupplier<T> supplier)
      throws Exception {
    return withRefreshToken(connectionProviderSupplier, supplier, MAX_REFRESH_ATTEMPTS);
  }

  /**
   * Gets the value provided by a {@link CheckedSupplier}, if that fails and the reason is that a token refresh is needed, the
   * refresh is performed up to {@code maxRefreshAttempts} times and the supplier is prompted to provide the value again.
   * <p>
   * If {@code maxRefreshAttempts} is lower than 1, no refresh or retries are performed
   *
   * @param connectionProviderSupplier a supplier of the connection provider that created a connection used in the given supplier
   *                                   to provide a value.
   * @param supplier                   a supplier that depends on an oauth based connection to provide a value.
   * @param maxRefreshAttempts         max amount of times that the refresh token operation should be executed and the operation
   *                                   retried.
   * @param <T>                        the type of the value to be provided
   * @return the value the supplier gives
   * @throws Exception
   * @since 4.4.0
   */
  public static <T> T withRefreshToken(Supplier<ConnectionProvider> connectionProviderSupplier,
                                       CheckedSupplier<T> supplier,
                                       int maxRefreshAttempts)
      throws Exception {
    try {
      return supplier.getChecked();
    } catch (Throwable e) {
      if (maxRefreshAttempts > 0 && refreshTokenIfNecessary(connectionProviderSupplier, e)) {
        return withRefreshToken(connectionProviderSupplier, supplier, --maxRefreshAttempts);
      } else if (e instanceof Exception) {
        throw (Exception) e;
      } else {
        return supplier.handleException(e);
      }
    }
  }

  /**
   * Gets the value provided by a {@link CheckedSupplier}, if that fails and the reason is that a token refresh is needed, the
   * refresh is performed and the supplier is prompted to provide the value one more time.
   *
   * @param connectionProvider the provider that created a connection used in the supplier to provide a value.
   * @param supplier           a supplier that depends on an oauth based connection to provide a value.
   * @param <T>                the type of the value to be provided
   * @return the value the supplier gives
   * @throws Exception
   * @since 4.4.0
   */
  public static <T> T withRefreshToken(ConnectionProvider connectionProvider, CheckedSupplier<T> supplier)
      throws Exception {
    return withRefreshToken(() -> connectionProvider, supplier);
  }

  /**
   * Performs a token refresh if the underlying {@link ConnectionProvider} of the given {@link ExecutionContextAdapter} knows how
   * do it and the given {@link Throwable} signals a refresh is needed by either being an {@link AccessTokenExpiredException} or
   * by one appearing in the chain of causes.
   *
   * @param operationContext connection provider that may be able to perform a token refresh.
   * @param e                throwable that may signal that a token refresh is needed.
   * @return if a token refresh was successfully performed or not.
   */
  public static boolean refreshTokenIfNecessary(ExecutionContextAdapter<OperationModel> operationContext, Throwable e) {
    OAuthConnectionProviderWrapper connectionProvider = getOAuthConnectionProvider(operationContext);
    return refreshTokenIfNecessary(() -> connectionProvider, e,
                                   of(new LazyValue<>(() -> format("at operation '%s:%s' using config '%s'",
                                                                   operationContext.getExtensionModel().getName(),
                                                                   operationContext.getComponentModel().getName(),
                                                                   operationContext.getConfiguration().get().getName()))),
                                   of(new LazyValue<>(() -> operationContext.getConfiguration().get().getName())));
  }

  /**
   * Performs a token refresh if the given {@link ConnectionProvider} knows how do it and the given {@link Throwable} signals a
   * refresh is needed by either being an {@link AccessTokenExpiredException} or by one appearing in the chain of causes.
   *
   * @param connectionProvider connection provider that may be able to perform a token refresh.
   * @param e                  throwable that may signal that a token refresh is needed.
   * @return if a token refresh was successfully performed or not.
   * @since 4.4.0
   */
  public static boolean refreshTokenIfNecessary(ConnectionProvider connectionProvider, Throwable e) {
    return refreshTokenIfNecessary(() -> connectionProvider, e, empty(), empty());
  }

  private static boolean refreshTokenIfNecessary(Supplier<ConnectionProvider> connectionProviderSupplier, Throwable e) {
    return refreshTokenIfNecessary(connectionProviderSupplier, e, empty(), empty());
  }

  private static boolean refreshTokenIfNecessary(Supplier<ConnectionProvider> connectionProviderSupplier, Throwable e,
                                                 Optional<LazyValue<String>> refreshContext,
                                                 Optional<LazyValue<String>> configName) {
    AccessTokenExpiredException expiredException = getTokenExpirationException(e);
    if (expiredException == null) {
      return false;
    }

    OAuthConnectionProviderWrapper oauthConnectionProvider = getOAuthConnectionProvider(connectionProviderSupplier.get());
    if (oauthConnectionProvider == null) {
      return false;
    }

    Reference<Optional<String>> resourceOwnerIdReference = new Reference<>(empty());
    oauthConnectionProvider.getGrantType().accept(new OAuthGrantTypeVisitor() {

      @Override
      public void visit(AuthorizationCodeGrantType grantType) {
        AuthorizationCodeConnectionProviderWrapper cp = (AuthorizationCodeConnectionProviderWrapper) oauthConnectionProvider;
        String rsId = cp.getResourceOwnerId();
        resourceOwnerIdReference.set(of(rsId));

        LOGGER.info("AccessToken for resourceOwner '{}' expired {}. "
            + "Will attempt to refresh token and retry", rsId, refreshContext.map(LazyValue::get).orElse(""));
      }

      @Override
      public void visit(ClientCredentialsGrantType grantType) {
        logTokenExpiration();
      }

      @Override
      public void visit(PlatformManagedOAuthGrantType grantType) {
        logTokenExpiration();
      }

      private void logTokenExpiration() {
        LOGGER.info("AccessToken expired {}. "
            + "Will attempt to refresh token and retry", refreshContext.map(LazyValue::get).orElse(""));
      }
    });

    Optional<String> resourceOwnerId = resourceOwnerIdReference.get();

    try {
      oauthConnectionProvider.refreshToken(resourceOwnerId.orElse(""));
    } catch (Exception refreshException) {
      String errorMessage = format("AccessToken %s expired %s. Refresh token workflow was attempted but failed.",
                                   forResourceOwner(resourceOwnerId),
                                   refreshContext.map(LazyValue::get).orElse(""));
      LOGGER.error(errorMessage, refreshException);
      throw new MuleRuntimeException(createStaticMessage(errorMessage), refreshException);
    }
    LOGGER.info("Access Token successfully refreshed {} on config '{}'",
                forResourceOwner(resourceOwnerId), configName.map(LazyValue::get).orElse(""));

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
