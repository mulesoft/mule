/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.injectRefName;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.connection.DefaultConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.ErrorTypeHandlerConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.PoolingConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ResolverSetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * Implementation of {@link ResolverSetBasedObjectBuilder} which produces instances of {@link ConnectionProviderModel}
 *
 * @since 4.0
 */
public class DefaultConnectionProviderObjectBuilder<C> extends ConnectionProviderObjectBuilder<C> {

  DefaultConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet,
                                         ExtensionModel extensionModel, MuleContext muleContext) {
    super(providerModel, resolverSet, extensionModel, muleContext);
  }

  public DefaultConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet,
                                                PoolingProfile poolingProfile,
                                                ReconnectionConfig reconnectionConfig,
                                                ExtensionModel extensionModel, MuleContext muleContext) {
    super(providerModel, resolverSet, poolingProfile, reconnectionConfig, extensionModel, muleContext);
  }

  @Override
  public final Pair<ConnectionProvider<C>, ResolverSetResult> build(ResolverSetResult result) throws MuleException {
    ConnectionProvider<C> provider = doBuild(result);

    provider = applyConnectionManagement(provider);
    provider = applyErrorHandling(provider);
    provider = getExtensionClassloaderAwareConnectionProvider(provider);

    return new Pair<>(provider, result);
  }

  protected ConnectionProvider<C> doBuild(ResolverSetResult result) throws MuleException {
    ConnectionProvider<C> provider = super.build(result).getFirst();
    injectRefName(providerModel, provider, ownerConfigName);

    return provider;
  }

  private ConnectionProvider<C> applyErrorHandling(ConnectionProvider<C> provider) {
    return new ErrorTypeHandlerConnectionProviderWrapper<>(provider, extensionModel, reconnectionConfig, muleContext);
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
   * Wraps the {@link ConnectionProvider} inside of a dynamic proxy which changes the current {@link ClassLoader} to the
   * the extension's {@link ClassLoader} when executing any method of this wrapped {@link ConnectionProvider}
   * <p>
   * This ensures that every time that the {@link ConnectionProvider} is used, it will work in the correct classloader.
   * <p>
   * Although if the {@link ConnectionProvider} is created with the correct classloader and then used with an incorrect
   * one this may work, due that static class references were loaded correctly, logic loading class in a dynamic way
   * will fail.
   *
   * @param provider The {@link ConnectionProvider} to wrap
   * @return The wrapped {@link ConnectionProvider}
   */
  private ConnectionProvider<C> getExtensionClassloaderAwareConnectionProvider(ConnectionProvider<C> provider) {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(DefaultConnectionProviderWrapper.class);
    enhancer.setCallback(new MethodInterceptor() {

      LazyValue<ClassLoader> extensionClassLoader = new LazyValue<>(() -> getClassLoader(extensionModel));

      @Override
      public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        Reference<Object> resultReference = new Reference<>();
        Reference<Throwable> errorReference = new Reference<>();

        withContextClassLoader(extensionClassLoader.get(), () -> {
          try {
            resultReference.set(methodProxy.invokeSuper(o, objects));
          } catch (Throwable t) {
            errorReference.set(t);
          }
        });

        if (errorReference.get() != null) {
          throw errorReference.get();
        } else {
          return resultReference.get();
        }
      }
    });

    provider = (ConnectionProvider<C>) enhancer.create(new Class[] {ConnectionProvider.class, MuleContext.class},
                                                       new Object[] {provider, muleContext});
    return provider;
  }

}
