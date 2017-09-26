/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.api.exception.MuleException;

/**
 * An implementation of {@link Injector} which uses a {@link RegistryProvider} to look for {@link Registry} instances which also
 * implement {@link Injector}. The injection operation is then delegated into the first matching registry.
 *
 * If no appropriate registry is found, then the injection operation does not take place.
 *
 * @since 3.7.0
 */
public class RegistryDelegatingInjector implements Injector {

  private final RegistryProvider registryProvider;

  public RegistryDelegatingInjector(RegistryProvider registryProvider) {
    checkArgument(registryProvider != null, "registryProvider cannot be null");
    this.registryProvider = registryProvider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T inject(T object) throws MuleException {
    for (Registry registry : registryProvider.getRegistries()) {
      if (registry instanceof Injector) {
        return ((Injector) registry).inject(object);
      }
    }

    return object;
  }
}
