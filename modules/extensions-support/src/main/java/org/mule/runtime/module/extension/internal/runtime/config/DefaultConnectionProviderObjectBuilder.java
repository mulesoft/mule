/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.injectFields;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.internal.connection.ErrorTypeHandlerConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.PoolingConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
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
                                         ExtensionModel extensionModel, ExpressionManager expressionManager,
                                         MuleContext muleContext) {
    super(providerModel, resolverSet, extensionModel, expressionManager, muleContext);
  }

  public DefaultConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet,
                                                PoolingProfile poolingProfile,
                                                ReconnectionConfig reconnectionConfig,
                                                ExtensionModel extensionModel,
                                                ExpressionManager expressionManager,
                                                MuleContext muleContext) {
    super(providerModel, resolverSet, poolingProfile, reconnectionConfig, extensionModel, expressionManager, muleContext);
  }

  public DefaultConnectionProviderObjectBuilder(Class<?> prototypeClass, ConnectionProviderModel providerModel,
                                                ResolverSet resolverSet,
                                                PoolingProfile poolingProfile,
                                                ReconnectionConfig reconnectionConfig,
                                                ExtensionModel extensionModel,
                                                ExpressionManager expressionManager,
                                                MuleContext muleContext) {
    super(prototypeClass, providerModel, resolverSet, poolingProfile, reconnectionConfig, extensionModel, expressionManager,
          muleContext);
  }

  @Override
  public final Pair<ConnectionProvider<C>, ResolverSetResult> build(ResolverSetResult result) throws MuleException {
    ConnectionProvider<C> provider = doBuild(result);

    muleContext.getInjector().inject(provider);
    provider = applyConnectionProviderClassLoaderProxy(provider);
    provider = applyConnectionManagement(provider);
    provider = applyErrorHandling(provider);

    return new Pair<>(provider, result);
  }

  protected ConnectionProvider<C> doBuild(ResolverSetResult result) throws MuleException {
    ConnectionProvider<C> provider = super.build(result).getFirst();
    injectFields(providerModel, provider, ownerConfigName, muleContext.getConfiguration().getDefaultEncoding());
    return provider;
  }

  private ConnectionProvider<C> applyErrorHandling(ConnectionProvider<C> provider) {
    return new ErrorTypeHandlerConnectionProviderWrapper<>(provider, extensionModel, reconnectionConfig,
                                                           muleContext.getErrorTypeRepository());
  }

  private ConnectionProvider<C> applyConnectionManagement(ConnectionProvider<C> provider) {
    final ConnectionManagementType connectionManagementType = providerModel.getConnectionManagementType();
    if (connectionManagementType == POOLING) {
      provider = new PoolingConnectionProviderWrapper<>(provider, poolingProfile, reconnectionConfig);
    } else {
      provider = new ReconnectableConnectionProviderWrapper<>(provider, reconnectionConfig);
    }
    return provider;
  }

  /**
   * Wraps the {@link ConnectionProvider} inside of a dynamic proxy which changes the current {@link ClassLoader} to a composition
   * of the extension's {@link ClassLoader} and the app classloader when executing any method of this wrapped
   * {@link ConnectionProvider}
   * <p>
   * This ensures that every time that the {@link ConnectionProvider} is used, it will work in the correct classloader.
   * <p>
   * Although if the {@link ConnectionProvider} is created with the correct classloader and then used with an incorrect one this
   * may work, due that static class references were loaded correctly, logic loading class in a dynamic way will fail.
   *
   * @param provider The {@link ConnectionProvider} to wrap
   * @return The wrapped {@link ConnectionProvider}
   */
  private ConnectionProvider<C> applyConnectionProviderClassLoaderProxy(ConnectionProvider provider) {
    final ClassLoader extensionClassLoader = currentThread().getContextClassLoader();
    final ClassLoader appRegionClassLoader = muleContext.getExecutionClassLoader().getParent();

    return ClassLoaderConnectionProviderWrapper
        .newInstance(provider, new CompositeClassLoader(extensionClassLoader, appRegionClassLoader));
  }

}
