/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service;

import static java.lang.String.format;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesTempFolder;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.api.util.Pair;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Discovers services artifacts from the {@link MuleFoldersUtil#SERVICES_FOLDER} folder.
 */
public class FileSystemServiceProviderDiscoverer implements ServiceProviderDiscoverer {

  private final ArtifactClassLoader apiClassLoader;
  private final ArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory;

  /**
   * Creates a new instance.
   *
   * @param containerClassLoader container artifact classLoader. Non null.
   * @param serviceClassLoaderFactory factory used to create service's classloaders. Non null.
   */
  public FileSystemServiceProviderDiscoverer(ArtifactClassLoader containerClassLoader,
                                             ArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory) {
    checkArgument(containerClassLoader != null, "containerClassLoader cannot be null");
    checkArgument(serviceClassLoaderFactory != null, "serviceClassLoaderFactory cannot be null");
    this.apiClassLoader = containerClassLoader;
    this.serviceClassLoaderFactory = serviceClassLoaderFactory;
  }

  @Override
  public List<Pair<ArtifactClassLoader, ServiceProvider>> discover() throws ServiceResolutionError {
    final ServiceDescriptorFactory serviceDescriptorFactory = new ServiceDescriptorFactory();

    final List<ServiceDescriptor> serviceDescriptors = new LinkedList<>();

    for (String serviceFile : getServicesFolder().list(new SuffixFileFilter(".zip"))) {
      final File tempFolder = new File(getServicesTempFolder(), getBaseName(serviceFile));
      try {
        unzip(new File(getServicesFolder(), serviceFile), tempFolder);
      } catch (IOException e) {
        throw new ServiceResolutionError("Error processing service ZIP file", e);
      }

      final ServiceDescriptor serviceDescriptor = serviceDescriptorFactory.create(tempFolder);
      serviceDescriptors.add(serviceDescriptor);
    }

    return createServiceProviders(serviceDescriptors, serviceClassLoaderFactory);
  }

  private List<Pair<ArtifactClassLoader, ServiceProvider>> createServiceProviders(List<ServiceDescriptor> serviceDescriptors,
                                                                                  ArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory)
      throws ServiceResolutionError {
    List<Pair<ArtifactClassLoader, ServiceProvider>> serviceProviders = new LinkedList<>();
    for (ServiceDescriptor serviceDescriptor : serviceDescriptors) {
      final ArtifactClassLoader serviceClassLoader =
          serviceClassLoaderFactory.create(getServiceArtifactId(serviceDescriptor), serviceDescriptor,
                                           apiClassLoader.getClassLoader(), apiClassLoader.getClassLoaderLookupPolicy());
      final ServiceProvider serviceProvider =
          instantiateServiceProvider(serviceClassLoader.getClassLoader(), serviceDescriptor.getServiceProviderClassName());

      serviceProviders.add(new Pair<>(serviceClassLoader, serviceProvider));
    }
    return serviceProviders;
  }

  private String getServiceArtifactId(ServiceDescriptor serviceDescriptor) {
    return "service/" + serviceDescriptor.getName();
  }

  private ServiceProvider instantiateServiceProvider(ClassLoader classLoader, String className) throws ServiceResolutionError {
    Object reflectedObject;
    try {
      reflectedObject = withContextClassLoader(classLoader, () -> {
        try {
          return ClassUtils.instantiateClass(className);
        } catch (Exception e) {
          throw new MuleRuntimeException(createStaticMessage("Unable to create service from class: " + className), e);
        }
      });
    } catch (RuntimeException e) {
      throw new ServiceResolutionError(e.getMessage());
    }

    if (!(reflectedObject instanceof ServiceProvider)) {
      throw new ServiceResolutionError(format("Provided service class '%s' does not implement '%s'", className,
                                              ServiceProvider.class.getName()));
    }

    return (ServiceProvider) reflectedObject;
  }

}
