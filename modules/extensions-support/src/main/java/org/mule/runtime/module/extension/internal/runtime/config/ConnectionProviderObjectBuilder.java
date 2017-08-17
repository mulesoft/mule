/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getConnectionProviderFactory;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ResolverSetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;

/**
 * Implementation of {@link ResolverSetBasedObjectBuilder} which produces instances of {@link ConnectionProviderModel}
 *
 * @since 4.0
 */
public abstract class ConnectionProviderObjectBuilder<C>
    extends ResolverSetBasedObjectBuilder<Pair<ConnectionProvider<C>, ResolverSetResult>> {

  protected final ConnectionProviderModel providerModel;
  protected final ReconnectionConfig reconnectionConfig;
  protected final PoolingProfile poolingProfile;
  protected final ExtensionModel extensionModel;
  protected final MuleContext muleContext;
  protected String ownerConfigName;

  /**
   * Creates a new instances which produces instances based on the given {@code providerModel} and {@code resolverSet}
   *
   * @param providerModel     the {@link ConnectionProviderModel} which describes the instances to be produced
   * @param resolverSet       a {@link ResolverSet} to populate the values
   */
  public ConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet,
                                         ExtensionModel extensionModel,
                                         MuleContext muleContext) {
    this(providerModel, resolverSet, null, null, extensionModel, muleContext);
  }

  public ConnectionProviderObjectBuilder(ConnectionProviderModel providerModel,
                                         ResolverSet resolverSet,
                                         PoolingProfile poolingProfile,
                                         ReconnectionConfig reconnectionConfig,
                                         ExtensionModel extensionModel, MuleContext muleContext) {
    super(getConnectionProviderFactory(providerModel).getObjectType(), providerModel, resolverSet);
    this.providerModel = providerModel;
    this.poolingProfile = poolingProfile;
    this.extensionModel = extensionModel;
    this.muleContext = muleContext;
    this.reconnectionConfig = computeReconnectionConfig(reconnectionConfig);
  }

  private ReconnectionConfig computeReconnectionConfig(ReconnectionConfig reconnectionConfig) {
    return reconnectionConfig != null ? reconnectionConfig : ReconnectionConfig.getDefault();
  }

  public ConnectionProviderObjectBuilder(ConnectionProviderModel providerModel,
                                         Class<?> prototypeClass,
                                         ResolverSet resolverSet,
                                         PoolingProfile poolingProfile,
                                         ReconnectionConfig reconnectionConfig,
                                         ExtensionModel extensionModel,
                                         MuleContext muleContext) {
    super(prototypeClass, providerModel, resolverSet);
    this.providerModel = providerModel;
    this.poolingProfile = poolingProfile;
    this.extensionModel = extensionModel;
    this.muleContext = muleContext;
    this.reconnectionConfig = computeReconnectionConfig(reconnectionConfig);
  }

  /**
   * {@inheritDoc}
   */
  protected Pair<ConnectionProvider<C>, ResolverSetResult> instantiateObject() {
    return new Pair<>((ConnectionProvider<C>) getConnectionProviderFactory(providerModel).newInstance(), null);
  }

  @Override
  public Pair<ConnectionProvider<C>, ResolverSetResult> build(ResolverSetResult result) throws MuleException {
    ConnectionProvider<C> value = instantiateObject().getFirst();
    populate(result, value);
    return new Pair<>(value, result);
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
