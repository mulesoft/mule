/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFields;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.util.func.Once;
import org.mule.runtime.core.api.util.func.Once.RunOnce;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthCallbackValue;
import org.mule.runtime.extension.api.connectivity.NoConnectivityTest;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.api.ClientCredentialsOAuthDancer;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * A {@link ReconnectableConnectionProviderWrapper} which makes sure that by the time the
 * {@link ConnectionProvider#connect()} method is invoked on the delegate, the authorization dance has
 * been completed and the {@link org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState} and {@link OAuthCallbackValue} fields have
 * been properly injected
 *
 * @since 4.2.1
 */
public class ClientCredentialsConnectionProviderWrapper<C> extends ReconnectableConnectionProviderWrapper<C>
    implements NoConnectivityTest {

  private final ClientCredentialsConfig oauthConfig;
  private final Map<Field, String> callbackValues;
  private final AuthorizationCodeOAuthHandler oauthHandler;
  private final FieldSetter<ConnectionProvider<C>, AuthorizationCodeState> authCodeStateSetter;
  private final RunOnce dance;

  private ClientCredentialsOAuthDancer dancer;

  public ClientCredentialsConnectionProviderWrapper(ConnectionProvider<C> delegate,
                                                    ClientCredentialsConfig oauthConfig,
                                                    Map<Field, String> callbackValues,
                                                    AuthorizationCodeOAuthHandler oauthHandler,
                                                    ReconnectionConfig reconnectionConfig) {
    super(delegate, reconnectionConfig);
    this.oauthConfig = oauthConfig;
    this.oauthHandler = oauthHandler;
    authCodeStateSetter = getAuthCodeStateSetter(delegate);
    this.callbackValues = unmodifiableMap(callbackValues);
    dance = Once.of(this::updateAuthState);
  }

  @Override
  public C connect() throws ConnectionException {
    dance.runOnce();
    return super.connect();
  }

  private void updateAuthState() {
    final ConnectionProvider<C> delegate = getDelegate();
    ResourceOwnerOAuthContext context = getContext();
    authCodeStateSetter
        .set(delegate, new UpdatingAuthorizationCodeState(oauthConfig, dancer, context,
                                                          updatedContext -> updateOAuthParameters(delegate, updatedContext)));
    updateOAuthParameters(delegate, context);
  }

  private void updateOAuthParameters(ConnectionProvider<C> delegate, ResourceOwnerOAuthContext context) {
    Map<String, Object> responseParameters = context.getTokenResponseParameters();
    callbackValues.keySet().forEach(field -> {
      String key = field.getName();
      if (responseParameters.containsKey(key)) {
        new FieldSetter<>(field).set(delegate, responseParameters.get(key));
      }
    });
  }

  public String getResourceOwnerId() {
    return getContext().getResourceOwnerId();
  }

  private FieldSetter<ConnectionProvider<C>, AuthorizationCodeState> getAuthCodeStateSetter(ConnectionProvider<C> delegate) {
    List<Field> stateFields = getFields(delegate.getClass()).stream()
        .filter(f -> f.getType().equals(AuthorizationCodeState.class))
        .collect(toList());

    if (stateFields.size() != 1) {
      throw new IllegalConnectionProviderModelDefinitionException(
          format("Connection Provider of class '%s' uses OAuth2 authorization code grant type and thus should contain "
                     + "one (and only one) field of type %s. %d were found",
                 delegate.getClass().getName(),
                 AuthorizationCodeState.class.getName(),
                 stateFields.size()));
    }

    return new FieldSetter<>(stateFields.get(0));
  }

  private ResourceOwnerOAuthContext getContext() {
    return oauthHandler.getOAuthContext(oauthConfig)
        .orElseThrow(() -> new IllegalArgumentException("OAuth authorization dance not yet performed for resourceOwnerId "
                                                            + oauthConfig.getResourceOwnerId()));
  }

  @Override
  public void start() throws MuleException {
    dancer = oauthHandler.register(oauthConfig);
    super.start();
  }
}
