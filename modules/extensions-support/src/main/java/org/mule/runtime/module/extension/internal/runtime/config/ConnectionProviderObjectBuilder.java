/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getConnectionProviderFactory;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.injectConfigName;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
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
public final class ConnectionProviderObjectBuilder<C> extends ResolverSetBasedObjectBuilder<ConnectionProvider<C>> {

  private final ConnectionProviderModel providerModel;
  private final boolean disableValidation;
  private final RetryPolicyTemplate retryPolicyTemplate;
  private final PoolingProfile poolingProfile;
  private final ExtensionModel extensionModel;
  private final MuleContext muleContext;

  private String ownerConfigName = "";

  /**
   * Creates a new instances which produces instances based on the given {@code providerModel} and {@code resolverSet}
   *
   * @param providerModel     the {@link ConnectionProviderModel} which describes the instances to be produced
   * @param resolverSet       a {@link ResolverSet} to populate the values
   * @param connectionManager a {@link ConnectionManagerAdapter} to obtain the default {@link RetryPolicyTemplate} in case of none
   *                          is provided
   */
  public ConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet,
                                         ConnectionManagerAdapter connectionManager, ExtensionModel extensionModel,
                                         MuleContext muleContext) {
    this(providerModel, resolverSet, null, false, null, connectionManager, extensionModel, muleContext);
  }

  public ConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet,
                                         PoolingProfile poolingProfile, boolean disableValidation,
                                         RetryPolicyTemplate retryPolicyTemplate, ConnectionManagerAdapter connectionManager,
                                         ExtensionModel extensionModel, MuleContext muleContext) {
    super(getConnectionProviderFactory(providerModel).getObjectType(), providerModel, resolverSet);
    this.providerModel = providerModel;
    this.poolingProfile = poolingProfile;
    this.extensionModel = extensionModel;
    this.muleContext = muleContext;
    this.retryPolicyTemplate =
        retryPolicyTemplate != null ? retryPolicyTemplate : connectionManager.getDefaultRetryPolicyTemplate();
    this.disableValidation = disableValidation;
  }

  @Override
  public ConnectionProvider<C> build(ResolverSetResult result) throws MuleException {
    ConnectionProvider<C> provider = super.build(result);
    injectConfigName(providerModel, provider, ownerConfigName);

    final ConnectionManagementType connectionManagementType = providerModel.getConnectionManagementType();
    if (connectionManagementType == POOLING) {
      provider = new PoolingConnectionProviderWrapper<>(provider, poolingProfile, disableValidation, retryPolicyTemplate);
    } else {
      provider = new ReconnectableConnectionProviderWrapper<>(provider, disableValidation, retryPolicyTemplate);
    }

    provider = new ErrorTypeHandlerConnectionProviderWrapper<>(provider, muleContext, extensionModel, retryPolicyTemplate);
    return provider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ConnectionProvider instantiateObject() {
    return getConnectionProviderFactory(providerModel).newInstance();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return resolverSet.isDynamic();
  }

  public void setOwnerConfigName(String ownerConfigName) {
    this.ownerConfigName = ownerConfigName;
  }
}
