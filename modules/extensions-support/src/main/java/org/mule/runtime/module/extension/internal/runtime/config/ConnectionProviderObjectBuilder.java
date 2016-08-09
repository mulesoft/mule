/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.extension.api.introspection.connection.ConnectionManagementType.CACHED;
import static org.mule.runtime.extension.api.introspection.connection.ConnectionManagementType.NONE;
import static org.mule.runtime.extension.api.introspection.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.injectConfigName;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.CachedConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.connection.PoolingConnectionProviderWrapper;
import org.mule.runtime.extension.api.introspection.connection.ConnectionManagementType;
import org.mule.runtime.extension.api.introspection.connection.RuntimeConnectionProviderModel;
import org.mule.runtime.module.extension.internal.runtime.ParameterGroupAwareObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;

/**
 * Implementation of {@link ParameterGroupAwareObjectBuilder} which produces instances of {@link RuntimeConnectionProviderModel}
 *
 * @since 4.0
 */
public final class ConnectionProviderObjectBuilder extends ParameterGroupAwareObjectBuilder<ConnectionProvider> {

  private final RuntimeConnectionProviderModel providerModel;
  private final boolean disableValidation;
  private final RetryPolicyTemplate retryPolicyTemplate;
  private final PoolingProfile poolingProfile;

  private String ownerConfigName = "";

  /**
   * Creates a new instances which produces instances based on the given {@code providerModel} and {@code resolverSet}
   *
   * @param providerModel the {@link RuntimeConnectionProviderModel} which describes the instances to be produced
   * @param resolverSet a {@link ResolverSet} to populate the values
   * @param connectionManager a {@link ConnectionManagerAdapter} to obtain the default {@link RetryPolicyTemplate} in case of none
   *        is provided
   */
  public ConnectionProviderObjectBuilder(RuntimeConnectionProviderModel providerModel, ResolverSet resolverSet,
                                         ConnectionManagerAdapter connectionManager) {
    this(providerModel, resolverSet, null, false, null, connectionManager);
  }

  public ConnectionProviderObjectBuilder(RuntimeConnectionProviderModel providerModel, ResolverSet resolverSet,
                                         PoolingProfile poolingProfile, boolean disableValidation,
                                         RetryPolicyTemplate retryPolicyTemplate, ConnectionManagerAdapter connectionManager) {
    super(providerModel.getConnectionProviderFactory().getObjectType(), providerModel, resolverSet);
    this.providerModel = providerModel;
    this.poolingProfile = poolingProfile;
    this.retryPolicyTemplate =
        retryPolicyTemplate != null ? retryPolicyTemplate : connectionManager.getDefaultRetryPolicyTemplate();
    this.disableValidation = disableValidation;
  }

  @Override
  public ConnectionProvider build(ResolverSetResult result) throws MuleException {
    ConnectionProvider provider = super.build(result);
    injectConfigName(providerModel, provider, ownerConfigName);

    final ConnectionManagementType connectionManagementType = providerModel.getConnectionManagementType();
    if (connectionManagementType == POOLING) {
      provider = new PoolingConnectionProviderWrapper(provider, poolingProfile, disableValidation, retryPolicyTemplate);
    } else if (connectionManagementType == CACHED) {
      provider = new CachedConnectionProviderWrapper(provider, disableValidation, retryPolicyTemplate);
    } else if (connectionManagementType != NONE) {
      throw new IllegalArgumentException("Unknown connection management type: " + connectionManagementType);
    }

    return provider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ConnectionProvider instantiateObject() {
    return providerModel.getConnectionProviderFactory().newInstance();
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
