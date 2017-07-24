/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.injectRefName;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.connection.ErrorTypeHandlerConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.PoolingConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ResolverSetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;

/**
 * Implementation of {@link ResolverSetBasedObjectBuilder} which produces instances of {@link ConnectionProviderModel}
 *
 * @since 4.0
 */
public class DefaultConnectionProviderObjectBuilder<C> extends ConnectionProviderObjectBuilder<C> {

  DefaultConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet,
                                         ConnectionManagerAdapter connectionManager,
                                         ExtensionModel extensionModel, MuleContext muleContext) {
    super(providerModel, resolverSet, connectionManager, extensionModel, muleContext);
  }

  public DefaultConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet,
                                                PoolingProfile poolingProfile, boolean disableValidation,
                                                RetryPolicyTemplate retryPolicyTemplate,
                                                ConnectionManagerAdapter connectionManager,
                                                ExtensionModel extensionModel, MuleContext muleContext) {
    super(providerModel, resolverSet, poolingProfile, disableValidation,
          retryPolicyTemplate, connectionManager, extensionModel, muleContext);
  }

  @Override
  public final ConnectionProvider<C> build(ResolverSetResult result) throws MuleException {
    ConnectionProvider<C> provider = doBuild(result);

    provider = applyConnectionManagement(provider);
    provider = applyErrorHandling(provider);
    return provider;
  }

  protected ConnectionProvider<C> doBuild(ResolverSetResult result) throws MuleException {
    ConnectionProvider<C> provider = super.build(result);
    injectRefName(providerModel, provider, ownerConfigName);

    return provider;
  }

  private ConnectionProvider<C> applyErrorHandling(ConnectionProvider<C> provider) {
    provider = new ErrorTypeHandlerConnectionProviderWrapper<>(provider, muleContext, extensionModel, retryPolicyTemplate);
    return provider;
  }

  private ConnectionProvider<C> applyConnectionManagement(ConnectionProvider<C> provider) {
    final ConnectionManagementType connectionManagementType = providerModel.getConnectionManagementType();
    if (connectionManagementType == POOLING) {
      provider = new PoolingConnectionProviderWrapper<>(provider, poolingProfile, disableValidation, retryPolicyTemplate);
    } else {
      provider = new ReconnectableConnectionProviderWrapper<>(provider, disableValidation, retryPolicyTemplate);
    }
    return provider;
  }

}
