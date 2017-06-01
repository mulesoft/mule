/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;


/**
 * Creates instances of {@link ConnectionProvider} based on a {@link #providerClass}
 *
 * @param <C> the generic type for the connections that the created {@link ConnectionProvider providers} produce
 * @since 4.0
 */
final class DefaultConnectionProviderFactory<C> implements ConnectionProviderFactory<C> {

  private final Class<? extends ConnectionProvider> providerClass;
  private final ClassLoader extensionClassLoader;

  /**
   * Creates a new instance which creates {@link ConnectionProvider} instances of the given {@code providerClass}
   *
   * @param providerClass the {@link Class} of the created {@link ConnectionProvider providers}
   * @param extensionClassLoader the {@link ClassLoader} on which the extension is loaded
   * @throws IllegalModelDefinitionException if {@code providerClass} doesn't implement the {@link ConnectionProvider} interface
   * @throws IllegalArgumentException if {@code providerClass} is not an instantiable type
   */
  DefaultConnectionProviderFactory(Class<?> providerClass, ClassLoader extensionClassLoader) {
    this.extensionClassLoader = extensionClassLoader;
    if (!ConnectionProvider.class.isAssignableFrom(providerClass)) {
      throw new IllegalConnectionProviderModelDefinitionException(String
          .format("Class '%s' was specified as a connection provider but it doesn't implement the '%s' interface",
                  providerClass.getName(), ConnectionProvider.class.getName()));
    }

    checkInstantiable(providerClass);
    this.providerClass = (Class<? extends ConnectionProvider>) providerClass;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionProvider<C> newInstance() {
    try {
      return (ConnectionProvider) withContextClassLoader(extensionClassLoader, providerClass::newInstance);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create connection provider of type "
          + providerClass.getName()), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends ConnectionProvider> getObjectType() {
    return providerClass;
  }
}
