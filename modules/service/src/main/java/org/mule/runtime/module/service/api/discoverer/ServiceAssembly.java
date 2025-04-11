/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.discoverer;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.module.service.internal.discoverer.LazyServiceAssembly;

import java.util.function.Supplier;

/**
 * Assembles a {@link Service} implemented through a service artifact providing all the pieces necessary to use it
 *
 * @since 4.2
 */
@NoImplement
public interface ServiceAssembly {

  /**
   * @return a new {@link ServiceAssemblyBuilder} for a lazily assembled service.
   *
   * @since 4.6
   */
  public static ServiceAssemblyBuilder lazyBuilder() {
    return LazyServiceAssembly.builder();
  }

  /**
   * @return The service name
   */
  String getName();

  /**
   * @return The {@link ServiceProvider} through which the service can be instantiated
   */
  ServiceProvider getServiceProvider();

  /**
   * @return the service's {@link ClassLoader}
   */
  ClassLoader getClassLoader();

  /**
   * @return The contract interface that is being fulfilled
   */
  Class<? extends Service> getServiceContract();

  /**
   * A builder to create {@link LazyServiceAssembly} instances
   *
   * @since 4.6
   */
  public static interface ServiceAssemblyBuilder {

    /**
     * Allows to set the new of the assembled {@link Service}
     *
     * @param name the {@link Service} name
     * @return {@code this} builder
     */
    ServiceAssemblyBuilder withName(String name);

    /**
     * Provides a {@link Supplier} to lazily create the service's {@link ClassLoader}.
     *
     * @param artifactClassLoader A {@link Supplier} to lazily create the service's {@link ClassLoader}
     * @return {@code this} builder
     */
    ServiceAssemblyBuilder withClassLoader(Supplier<ClassLoader> artifactClassLoader);

    /**
     * Provides a {@link Supplier} to lazily create the service's {@link ServiceProvider}.
     *
     * @param serviceProviderSupplier A {@link Supplier} to lazily create the service's {@link ServiceProvider}
     * @return {@code this} builder
     */
    ServiceAssemblyBuilder withServiceProvider(CheckedSupplier<ServiceProvider> serviceProviderSupplier);

    /**
     * Sets the classname of the {@link Service} contract that will be satisfied by the assembly
     *
     * @param contractClassName the {@link Service} classname
     * @return {@code this} builder
     */
    ServiceAssemblyBuilder forContract(String contractClassName);

    /**
     * @return a new {@link ServiceAssembly} instance
     * @throws ServiceResolutionError if the assembly couldn't be created
     */
    ServiceAssembly build() throws ServiceResolutionError;

  }

}
