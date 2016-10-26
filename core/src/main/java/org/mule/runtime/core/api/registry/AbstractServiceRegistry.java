/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.registry;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.join;

import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Base implementation of {@link ServiceRegistry} that enforce common behaviour for all implementations.
 */
public abstract class AbstractServiceRegistry implements ServiceRegistry {

  /**
   * If {@code classLoader} is {@code null}, then the current {@link Thread#getContextClassLoader()} will be used {@inheritDoc}
   */
  @Override
  public final <T> Collection<T> lookupProviders(Class<T> providerClass, ClassLoader classLoader) {
    if (classLoader == null) {
      classLoader = Thread.currentThread().getContextClassLoader();
    }

    return copyOf(doLookupProviders(providerClass, classLoader));
  }

  /**
   * Retrieves a collection of providers. This method will be reused to actually implement {@code {@link #lookupProviders(Class, ClassLoader)},
   * and {@code {@link #lookupProvider(Class, ClassLoader)}}}.
   *
   * @param providerClass a <code>Class</code>object indicating the class or interface of the service providers being detected.
   * @param loader the class loader to be used to load provider/configuration files and instantiate provider instances. If
   *        {@code null}, it will be up to the implementation to choose a {@link ClassLoader}
   * @return A {@link Collection} that yields provider objects for the given service, in some arbitrary order.
   */
  protected abstract <T> Collection<T> doLookupProviders(Class<T> providerClass, ClassLoader loader);

  /**
   * {@inheritDoc}
   */
  @Override
  public final <T> T lookupProvider(Class<T> providerClass, ClassLoader classLoader) {
    Collection<T> providers = lookupProviders(providerClass, classLoader);
    if (providers.isEmpty()) {
      throw new IllegalStateException("No provider found for class " + providerClass.getName());
    }
    if (providers.size() > 1) {
      List<String> providersNames =
          providers.stream().map(provider -> provider.getClass().getName()).collect(Collectors.toList());
      throw new IllegalStateException(format("More than one provided found for class %s, providers found are: %s",
                                             providerClass.getName(), join(providersNames, ",")));
    }
    return providers.iterator().next();
  }


}
