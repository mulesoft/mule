/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;

import static java.security.AccessController.doPrivileged;
import static java.security.AccessController.getContext;
import static java.util.Optional.empty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;

import java.io.File;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.mule.runtime.api.deployment.meta.MuleServiceContractModel;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceProviderDiscoverer;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.runtime.module.service.internal.artifact.ServiceDescriptor;
import org.mule.runtime.module.service.internal.artifact.ServiceDescriptorFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Discovers services artifacts from the {@link MuleFoldersUtil#SERVICES_FOLDER} folder.
 */
public class FileSystemServiceProviderDiscoverer implements ServiceProviderDiscoverer {

  private static final AccessControlContext ACCESS_CONTROL_CTX = getContext();

  private final ArtifactClassLoader apiClassLoader;
  private final ArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory;
  private final DescriptorLoaderRepository descriptorLoaderRepository;
  private final ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder;
  private final Supplier<File> targetServicesFolder;

  /**
   * Creates a new instance.
   *
   * @param containerClassLoader               container artifact classLoader. Non null.
   * @param serviceClassLoaderFactory          factory used to create service's classloaders. Non null.
   * @param descriptorLoaderRepository         contains all the {@link ClassLoaderModelLoader} registered on the container. Non null
   * @param artifactDescriptorValidatorBuilder {@link ArtifactDescriptorValidatorBuilder} to create the {@link org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator} in order to check the state of the descriptor once loaded.
   * @param targetServicesFolder               {@link File} where services are exploded and would be discovered. Non null.
   */
  public FileSystemServiceProviderDiscoverer(ArtifactClassLoader containerClassLoader,
                                             ArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory,
                                             DescriptorLoaderRepository descriptorLoaderRepository,
                                             ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder,
                                             File targetServicesFolder) {
    this(containerClassLoader, serviceClassLoaderFactory, descriptorLoaderRepository, artifactDescriptorValidatorBuilder,
         () -> targetServicesFolder);
  }

  /**
   * Creates a new instance that discover services from Mule Runtime services folder.
   *
   * @param containerClassLoader               container artifact classLoader. Non null.
   * @param serviceClassLoaderFactory          factory used to create service's classloaders. Non null.
   * @param descriptorLoaderRepository         contains all the {@link ClassLoaderModelLoader} registered on the container. Non null
   * @param artifactDescriptorValidatorBuilder {@link ArtifactDescriptorValidatorBuilder} to create the {@link org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator} in order to check the state of the descriptor once loaded.
   */
  public FileSystemServiceProviderDiscoverer(ArtifactClassLoader containerClassLoader,
                                             ArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory,
                                             DescriptorLoaderRepository descriptorLoaderRepository,
                                             ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    this(containerClassLoader, serviceClassLoaderFactory, descriptorLoaderRepository, artifactDescriptorValidatorBuilder,
         () -> MuleFoldersUtil.getServicesFolder());
  }

  // Supplier in order to resolve the services folder after initialization due use MULE_HOME and tests set
  // this value after these discoverer is created
  private FileSystemServiceProviderDiscoverer(ArtifactClassLoader containerClassLoader,
                                              ArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory,
                                              DescriptorLoaderRepository descriptorLoaderRepository,
                                              ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder,
                                              Supplier<File> targetServicesFolder) {
    checkArgument(containerClassLoader != null, "containerClassLoader cannot be null");
    checkArgument(serviceClassLoaderFactory != null, "serviceClassLoaderFactory cannot be null");
    checkArgument(artifactDescriptorValidatorBuilder != null, "artifactDescriptorValidatorBuilder cannot be null");
    checkArgument(targetServicesFolder != null, "targetServicesFolder cannot be null");

    this.descriptorLoaderRepository = descriptorLoaderRepository;
    this.apiClassLoader = containerClassLoader;
    this.serviceClassLoaderFactory = serviceClassLoaderFactory;
    this.artifactDescriptorValidatorBuilder = artifactDescriptorValidatorBuilder;
    this.targetServicesFolder = targetServicesFolder;

  }

  @Override
  public List<ServiceAssembly> discover() throws ServiceResolutionError {
    final ServiceDescriptorFactory serviceDescriptorFactory =
        new ServiceDescriptorFactory(descriptorLoaderRepository, artifactDescriptorValidatorBuilder);

    final List<ServiceDescriptor> serviceDescriptors = getServiceDescriptors(serviceDescriptorFactory);

    return assemble(serviceDescriptors, serviceClassLoaderFactory);
  }

  private List<ServiceDescriptor> getServiceDescriptors(ServiceDescriptorFactory serviceDescriptorFactory)
      throws ServiceResolutionError {

    final File[] serviceDirectories = this.targetServicesFolder.get().listFiles(File::isDirectory);
    List<ServiceDescriptor> foundServices = new ArrayList<>(serviceDirectories.length);

    for (File serviceDirectory : serviceDirectories) {
      try {
        final ServiceDescriptor serviceDescriptor = serviceDescriptorFactory.create(serviceDirectory, empty());
        foundServices.add(serviceDescriptor);
      } catch (Exception e) {
        throw new ServiceResolutionError("Error processing service JAR file", unwrap(e));
      }
    }
    return foundServices;
  }

  private List<ServiceAssembly> assemble(List<ServiceDescriptor> serviceDescriptors,
                                         ArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory)
      throws ServiceResolutionError {

    List<ServiceAssembly> assemblies = new ArrayList<>(serviceDescriptors.size());
    for (ServiceDescriptor serviceDescriptor : serviceDescriptors) {

      final Supplier<ClassLoader> serviceClassLoader = new LazyValue<>(
                                                                       () -> (ClassLoader) doPrivileged((PrivilegedAction<ClassLoader>) () -> (ClassLoader) serviceClassLoaderFactory
                                                                               .create(getServiceArtifactId(serviceDescriptor),
                                                                                       serviceDescriptor,
                                                                                       apiClassLoader.getClassLoader(),
                                                                                       apiClassLoader
                                                                                           .getClassLoaderLookupPolicy()),
                                                                                         ACCESS_CONTROL_CTX));

      for (MuleServiceContractModel contract : serviceDescriptor.getContractModels()) {
        ServiceAssembly assembly = LazyServiceAssembly.builder()
            .withName(serviceDescriptor.getName())
            .withClassLoader(serviceClassLoader)
            .withServiceProvider(() -> instantiateServiceProvider(contract))
            .forContract(contract.getContractClassName())
            .build();

        assemblies.add(assembly);
      }
    }

    return assemblies;
  }

  private String getServiceArtifactId(ServiceDescriptor serviceDescriptor) {
    return "service/" + serviceDescriptor.getName();
  }

  private ServiceProvider instantiateServiceProvider(MuleServiceContractModel contractModel) throws ServiceResolutionError {
    final String className = contractModel.getServiceProviderClassName();
    Object reflectedObject;
    try {
      reflectedObject = ClassUtils.instantiateClass(className);
    } catch (Exception e) {
      throw new ServiceResolutionError("Unable to create service from class: " + className, e);
    }

    if (!(reflectedObject instanceof ServiceProvider)) {
      throw new ServiceResolutionError(String.format("Provided service class '%s' does not implement '%s'", className,
                                                     ServiceProvider.class.getName()));
    }

    return (ServiceProvider) reflectedObject;
  }

}
