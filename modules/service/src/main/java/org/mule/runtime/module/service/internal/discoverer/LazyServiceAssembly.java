/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.internal.discoverer;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;

import java.util.function.Supplier;

/**
 * A {@link ServiceAssembly} implementation which lazily creates its part. The Classloader and {@link ServiceProvider} won't
 * actually be created until actually needed.
 * <p>
 * Instances are only to be created through the {@link #builder()} method.
 *
 * @since 4.2
 */
public class LazyServiceAssembly implements ServiceAssembly {

  /**
   * @return a new {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A non-reusable builder to create {@link LazyServiceAssembly} instances
   *
   * @since 4.2
   */
  public static class Builder {

    private String name;
    private Supplier<ClassLoader> artifactClassLoader;
    private Supplier<ServiceProvider> serviceProviderSupplier;
    private String contractClassName;

    private Builder() {}

    /**
     * Allows to set the new of the assembled {@link Service}
     *
     * @param name the {@link Service} name
     * @return {@code this} builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * Provides a {@link Supplier} to lazily create the service's {@link ClassLoader}.
     *
     * @param artifactClassLoader A {@link Supplier} to lazily create the service's {@link ClassLoader}
     * @return {@code this} builder
     */
    public Builder withClassLoader(Supplier<ClassLoader> artifactClassLoader) {
      this.artifactClassLoader = artifactClassLoader;
      return this;
    }

    /**
     * Provides a {@link Supplier} to lazily create the service's {@link ServiceProvider}.
     *
     * @param serviceProviderSupplier A {@link Supplier} to lazily create the service's {@link ServiceProvider}
     * @return {@code this} builder
     */
    public Builder withServiceProvider(CheckedSupplier<ServiceProvider> serviceProviderSupplier) {
      this.serviceProviderSupplier = serviceProviderSupplier;
      return this;
    }

    /**
     * Sets the classname of the {@link Service} contract that will be satisfied by the assembly
     *
     * @param contractClassName the {@link Service} classname
     * @return {@code this} builder
     */
    public Builder forContract(String contractClassName) {
      this.contractClassName = contractClassName;
      return this;
    }

    /**
     * @return a new {@link LazyServiceAssembly} instance
     * @throws ServiceResolutionError if the assembly couldn't be created
     */
    public ServiceAssembly build() throws ServiceResolutionError {
      try {
        return new LazyServiceAssembly(name, artifactClassLoader, serviceProviderSupplier, resolveContract());
      } catch (Exception e) {
        throw new ServiceResolutionError("Could not load service " + name, e);
      }
    }

    private Class<? extends Service> resolveContract() {
      try {
        return loadClass(contractClassName, FileSystemServiceProviderDiscoverer.class);
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    }
  }


  private final String name;
  private final LazyValue<ClassLoader> classLoader;
  private final LazyValue<ServiceProvider> serviceProvider;
  private final Class<? extends Service> serviceContract;

  private LazyServiceAssembly(String name,
                              Supplier<ClassLoader> classLoader,
                              Supplier<ServiceProvider> serviceProvider,
                              Class<? extends Service> satisfiedContract) {
    checkArgument(!isBlank(name), "name cannot be blank");
    checkArgument(classLoader != null, "Classloader cannot be null");
    checkArgument(serviceProvider != null, "ServiceProvider supplier cannot be null");
    checkArgument(satisfiedContract != null, "satisfied contract cannot be null");

    this.name = name;
    this.classLoader = lazy(classLoader);
    this.serviceProvider = lazy(serviceProvider);
    this.serviceContract = satisfiedContract;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ServiceProvider getServiceProvider() {
    return serviceProvider.get();
  }

  @Override
  public ClassLoader getClassLoader() {
    return classLoader.get();
  }

  @Override
  public Class<? extends Service> getServiceContract() {
    return serviceContract;
  }

  private <T> LazyValue<T> lazy(Supplier<T> supplier) {
    if (supplier instanceof LazyValue) {
      return (LazyValue<T>) supplier;
    }

    return new LazyValue<>(supplier);
  }
}
