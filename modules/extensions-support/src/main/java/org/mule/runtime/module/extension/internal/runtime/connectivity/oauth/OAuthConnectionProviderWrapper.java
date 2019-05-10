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
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public abstract class OAuthConnectionProviderWrapper<C> extends ReconnectableConnectionProviderWrapper<C> {

  protected final Map<Field, String> callbackValues;

  public OAuthConnectionProviderWrapper(ConnectionProvider<C> delegate,
                                        ReconnectionConfig reconnectionConfig,
                                        Map<Field, String> callbackValues) {
    super(delegate, reconnectionConfig);
    this.callbackValues = unmodifiableMap(callbackValues);
  }

  public abstract OAuthGrantType getGrantType();

  public abstract void refreshToken(String resourceOwnerId);

  public abstract void invalidate(String resourceOwnerId);

  protected <T> FieldSetter<ConnectionProvider<C>, T> getOAuthStateSetter(ConnectionProvider<C> delegate,
                                                                          Class<T> stateType,
                                                                          String grantTypeName) {
    List<Field> stateFields = getFields(delegate.getClass()).stream()
        .filter(f -> f.getType().equals(stateType))
        .collect(toList());

    if (stateFields.size() != 1) {
      throw new IllegalConnectionProviderModelDefinitionException(
          format("Connection Provider of class '%s' uses OAuth2 %s grant type and thus should contain "
                     + "one (and only one) field of type %s. %d were found",
                 delegate.getClass().getName(),
                 grantTypeName,
                 stateType,
                 stateFields.size()));
    }

    return new FieldSetter<>(stateFields.get(0));
  }

  protected void updateOAuthParameters(ConnectionProvider<C> delegate, ResourceOwnerOAuthContext context) {
    Map<String, Object> responseParameters = context.getTokenResponseParameters();
    callbackValues.keySet().forEach(field -> {
      String key = field.getName();
      if (responseParameters.containsKey(key)) {
        new FieldSetter<>(field).set(delegate, responseParameters.get(key));
      }
    });
  }
}
