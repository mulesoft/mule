/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

public class LazyServiceAssembly implements ServiceAssembly {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private Supplier<ClassLoader> artifactClassLoader;
    private Supplier<ServiceProvider> serviceProviderSupplier;
    private String contractClassName;

    private Builder() {}

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withClassLoader(Supplier<ClassLoader> artifactClassLoader) {
      this.artifactClassLoader = artifactClassLoader;
      return this;
    }

    public Builder withServiceProvider(CheckedSupplier<ServiceProvider> serviceProviderSupplier) {
      this.serviceProviderSupplier = serviceProviderSupplier;
      return this;
    }

    public Builder forContract(String contractClassName) {
      this.contractClassName = contractClassName;
      return this;
    }

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
