/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.loader.parser.java.connection.SdkConnectionProviderAdapter.from;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.loader.parser.java.connection.SdkConnectionProviderAdapter;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.lang.ref.WeakReference;


/**
 * Creates instances of {@link ConnectionProvider} based on a {@link #providerClass}
 *
 * @param <C> the generic type for the connections that the created {@link ConnectionProvider providers} produce
 * @since 4.0
 */
public final class DefaultConnectionProviderFactory<C> implements ConnectionProviderFactory<C> {

  private final WeakReference<Class<?>> providerClass;
  private final WeakReference<ClassLoader> extensionClassLoader;

  /**
   * Creates a new instance which creates {@link ConnectionProvider} instances of the given {@code providerClass}
   *
   * @param providerClass        the {@link Class} of the created {@link ConnectionProvider providers}
   * @param extensionClassLoader the {@link ClassLoader} on which the extension is loaded
   * @throws IllegalModelDefinitionException if {@code providerClass} doesn't implement the {@link ConnectionProvider} interface
   * @throws IllegalArgumentException        if {@code providerClass} is not an instantiable type
   */
  public DefaultConnectionProviderFactory(Class<?> providerClass, ClassLoader extensionClassLoader) {
    this.extensionClassLoader = new WeakReference<>(extensionClassLoader);

    if (!isConnectionProvider(providerClass)) {
      throw new IllegalConnectionProviderModelDefinitionException(format(
                                                                         "Class '%s' was specified as a connection provider but it doesn't implement neither the '%s' or '%s' interfaces",
                                                                         providerClass.getName(),
                                                                         ConnectionProvider.class.getName(),
                                                                         org.mule.sdk.api.connectivity.ConnectionProvider.class
                                                                             .getName()));
    }

    checkInstantiable(providerClass, new ReflectionCache());
    this.providerClass = new WeakReference<>(providerClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionProvider<C> newInstance() {
    try {
      return withContextClassLoader(extensionClassLoader.get(), () -> from(providerClass.get().newInstance()));
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create connection provider of type "
          + providerClass.get().getName()), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends ConnectionProvider> getObjectType() {
    Class<?> type = providerClass.get();

    if (type == null) {
      return null;
    }

    return ConnectionProvider.class.isAssignableFrom(type)
        ? (Class<? extends ConnectionProvider>) type
        : SdkConnectionProviderAdapter.class;
  }

  private boolean isConnectionProvider(Class<?> providerClass) {
    return ConnectionProvider.class.isAssignableFrom(providerClass)
        || org.mule.sdk.api.connectivity.ConnectionProvider.class.isAssignableFrom(providerClass);
  }
}
