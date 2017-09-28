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
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.toAuthorizationCodeState;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFields;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.util.func.Once;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthCallbackValue;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.extension.api.connectivity.NoConnectivityTest;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * A {@link ReconnectableConnectionProviderWrapper} which makes sure that by the time the
 * {@link ConnectionProvider#connect()} method is invoked on the delegate, the authorization dance has
 * been completed and the {@link AuthorizationCodeState} and {@link OAuthCallbackValue} fields have
 * been properly injected
 *
 * @since 4.0
 */
public class OAuthConnectionProviderWrapper<C> extends ReconnectableConnectionProviderWrapper<C> implements NoConnectivityTest {

  private final OAuthConfig oauthConfig;
  private final Map<Field, String> callbackValues;
  private final ExtensionsOAuthManager oauthManager;
  private final FieldSetter<ConnectionProvider<C>, AuthorizationCodeState> authCodeStateSetter;
  private final Once dance;

  public OAuthConnectionProviderWrapper(ConnectionProvider<C> delegate,
                                        OAuthConfig oauthConfig,
                                        Map<Field, String> callbackValues,
                                        ExtensionsOAuthManager oauthManager,
                                        ReconnectionConfig reconnectionConfig) {
    super(delegate, reconnectionConfig);
    this.oauthConfig = oauthConfig;
    this.oauthManager = oauthManager;
    authCodeStateSetter = getAuthCodeStateSetter(delegate);
    this.callbackValues = unmodifiableMap(callbackValues);
    dance = Once.of(this::updateAuthState);
  }

  @Override
  public C connect() throws ConnectionException {
    dance.runOnce();
    return super.connect();
  }

  public void updateAuthState() {
    ResourceOwnerOAuthContext context = getContext();

    final ConnectionProvider<C> delegate = getDelegate();
    authCodeStateSetter.set(delegate, toAuthorizationCodeState(oauthConfig, context));

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
    return oauthManager.getOAuthContext(oauthConfig)
        .orElseThrow(() -> new IllegalArgumentException("OAuth authorization dance not yet performed for resourceOwnerId "
            + oauthConfig.getAuthCodeConfig().getResourceOwnerId()));
  }

  @Override
  public void start() throws MuleException {
    oauthManager.register(oauthConfig);
    super.start();
  }
}
