/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static net.sf.cglib.proxy.Enhancer.registerStaticCallbacks;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.injectFields;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.connection.ErrorTypeHandlerConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.HasDelegate;
import org.mule.runtime.core.internal.connection.PoolingConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ResolverSetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;

import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import net.sf.cglib.proxy.CallbackHelper;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;


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

    muleContext.getInjector().inject(provider);
    provider = applyExtensionClassLoaderProxy(provider);
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
   * Wraps the {@link ConnectionProvider} inside of a dynamic proxy which changes the current {@link ClassLoader} to the the
   * extension's {@link ClassLoader} when executing any method of this wrapped {@link ConnectionProvider}
   * <p>
   * This ensures that every time that the {@link ConnectionProvider} is used, it will work in the correct classloader.
   * <p>
   * Although if the {@link ConnectionProvider} is created with the correct classloader and then used with an incorrect one this
   * may work, due that static class references were loaded correctly, logic loading class in a dynamic way will fail.
   *
   * @param provider The {@link ConnectionProvider} to wrap
   * @return The wrapped {@link ConnectionProvider}
   */
  private ConnectionProvider<C> applyExtensionClassLoaderProxy(ConnectionProvider provider) {
    Enhancer enhancer = new Enhancer();
    ClassLoader classLoader = getClassLoader(extensionModel);

    Class[] proxyInterfaces = getProxyInterfaces(provider);
    enhancer.setInterfaces(proxyInterfaces);

    MethodInterceptor returnProviderInterceptor = (obj, method, args, proxy) -> provider;

    MethodInterceptor invokerInterceptor = (obj, method, args, proxy) -> {
      Reference<Object> resultReference = new Reference<>();
      Reference<Throwable> errorReference = new Reference<>();

      withContextClassLoader(classLoader, () -> {
        try {
          resultReference.set(method.invoke(provider, args));
        } catch (InvocationTargetException e) {
          errorReference.set(e.getTargetException());
        } catch (Throwable t) {
          errorReference.set(t);
        }
      });

      if (errorReference.get() != null) {
        throw errorReference.get();
      } else {
        return resultReference.get();
      }
    };

    CallbackHelper callbackHelper = new CallbackHelper(Object.class, proxyInterfaces) {

      @Override
      protected Object getCallback(Method method) {
        if (method.getDeclaringClass().equals(HasDelegate.class) && method.getName().equals("getDelegate")) {
          return returnProviderInterceptor;
        } else {
          return invokerInterceptor;
        }
      }
    };

    enhancer.setCallbackTypes(callbackHelper.getCallbackTypes());
    enhancer.setCallbackFilter(callbackHelper);

    if (Enhancer.class.getClassLoader() != classLoader) {
      enhancer.setClassLoader(new CompositeClassLoader(DefaultConnectionProviderObjectBuilder.class.getClassLoader(),
                                                       classLoader));
      enhancer.setUseCache(false);
    }

    Class<ConnectionProvider<C>> proxyClass = enhancer.createClass();
    registerStaticCallbacks(proxyClass, callbackHelper.getCallbacks());

    try {
      return proxyClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * @param provider the provider implementation
   * @return the interfaces that must be proxied by the runtime. Only runtime interfaces are considered since are the only ones
   *         that the runtime will invoke.
   */
  private Class[] getProxyInterfaces(ConnectionProvider provider) {
    List<Class<?>> originalInterfaces = ClassUtils.getAllInterfaces(provider.getClass());
    Class<?>[] runtimeInterfaces = filterNonRuntimeInterfaces(originalInterfaces);
    int length = runtimeInterfaces.length;
    Class[] newInterfaces = new Class[length + 1];

    boolean alreadyExists = false;

    for (int i = 0; i < length; i++) {
      Class<?> anInterface = runtimeInterfaces[i];
      if (anInterface.equals(HasDelegate.class)) {
        alreadyExists = true;
      }
      newInterfaces[i] = anInterface;
    }

    if (!alreadyExists) {
      newInterfaces[length] = HasDelegate.class;
      return newInterfaces;
    } else {
      return runtimeInterfaces;
    }
  }

  private Class<?>[] filterNonRuntimeInterfaces(List<Class<?>> interfaces) {
    return interfaces.stream().filter(clazz -> {
      try {
        Initialisable.class.getClassLoader().loadClass(clazz.getName());
        return true;
      } catch (ClassNotFoundException e) {
        return false;
      }
    }).toArray(Class[]::new);
  }
}
