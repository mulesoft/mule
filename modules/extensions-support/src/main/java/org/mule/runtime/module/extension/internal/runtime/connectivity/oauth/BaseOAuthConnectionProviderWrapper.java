/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.validateOAuthConnection;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Base class for an OAuth enabled {@link ConnectionProviderWrapper}.
 *
 * @param <C> the generic type of the returned connection
 * @since 4.3.0
 */
public abstract class BaseOAuthConnectionProviderWrapper<C> extends ReconnectableConnectionProviderWrapper<C> implements
    OAuthConnectionProviderWrapper<C> {

  protected final Map<Field, String> callbackValues;

  public BaseOAuthConnectionProviderWrapper(ConnectionProvider<C> delegate,
                                            ReconnectionConfig reconnectionConfig,
                                            Map<Field, String> callbackValues) {
    super(delegate, reconnectionConfig);
    this.callbackValues = unmodifiableMap(callbackValues);
  }

  @Override
  public ConnectionValidationResult validate(C connection) {
    return validateOAuthConnection(getDelegate(), connection, getContext());
  }

  @Override
  public String getResourceOwnerId() {
    return getContext().getResourceOwnerId();
  }

  protected abstract ResourceOwnerOAuthContext getContext();
}
