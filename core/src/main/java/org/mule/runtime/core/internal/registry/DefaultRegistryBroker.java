/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.lifecycle.DefaultLifecycleInterceptor;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @deprecated as of 3.7.0. This will be removed in Mule 4.0
 */
@Deprecated
public class DefaultRegistryBroker extends AbstractRegistryBroker {

  private final List<Registry> registries = new CopyOnWriteArrayList<>();
  private final AtomicReference<LifecycleRegistry> lifecycleRegistry = new AtomicReference<>(null);

  public DefaultRegistryBroker(MuleContext context, LifecycleInterceptor lifecycleInterceptor) {
    super(context, lifecycleInterceptor);
    addRegistry(new SimpleRegistry(context, lifecycleInterceptor));
  }

  @Override
  public void addRegistry(Registry registry) {
    registries.add(0, registry);
    lifecycleRegistry.set(null);
  }

  @Override
  public void removeRegistry(Registry registry) {
    registries.remove(registry);
    if (registry instanceof LifecycleRegistry) {
      lifecycleRegistry.compareAndSet((LifecycleRegistry) registry, null);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Registry> getRegistries() {
    return ImmutableList.copyOf(registries);
  }

  public LifecycleRegistry getLifecycleRegistry() {
    LifecycleRegistry cachedLifecycleRegistry = lifecycleRegistry.get();
    if (cachedLifecycleRegistry == null) {
      for (Registry registry : registries) {
        if (registry instanceof LifecycleRegistry) {
          cachedLifecycleRegistry = (LifecycleRegistry) registry;
          return lifecycleRegistry.compareAndSet(null, cachedLifecycleRegistry) ? cachedLifecycleRegistry
              : lifecycleRegistry.get();
        }
      }
    }

    return cachedLifecycleRegistry;
  }
}
