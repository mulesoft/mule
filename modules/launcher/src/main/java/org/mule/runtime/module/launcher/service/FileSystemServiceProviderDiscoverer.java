/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;

import static java.lang.String.format;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ClassUtils.instanciateClass;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.util.FileUtils.unzip;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getServicesTempFolder;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.launcher.MuleFoldersUtil;

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
  private ServiceClassLoaderFactory serviceClassLoaderFactory = new ServiceClassLoaderFactory();

  /**
   * Creates a new instance.
   *
   * @param containerClassLoader container artifact classLoader. Non null.
   * @param serviceClassLoaderFactory factory used to create service's classloaders. Non null.
   */
  public FileSystemServiceProviderDiscoverer(ArtifactClassLoader containerClassLoader,
                                             ServiceClassLoaderFactory serviceClassLoaderFactory) {
    checkArgument(containerClassLoader != null, "containerClassLoader cannot be null");
    checkArgument(serviceClassLoaderFactory != null, "serviceClassLoaderFactory cannot be null");
    this.apiClassLoader = containerClassLoader;
    this.serviceClassLoaderFactory = serviceClassLoaderFactory;
  }

  @Override
  public List<ServiceProvider> discover() throws ServiceResolutionError {
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

  private List<ServiceProvider> createServiceProviders(List<ServiceDescriptor> serviceDescriptors,
                                                       ServiceClassLoaderFactory serviceClassLoaderFactory)
      throws ServiceResolutionError {
    List<ServiceProvider> serviceProviders = new LinkedList<>();
    for (ServiceDescriptor serviceDescriptor : serviceDescriptors) {
      final ArtifactClassLoader serviceClassLoader = serviceClassLoaderFactory.create(apiClassLoader, serviceDescriptor);
      final ServiceProvider serviceProvider =
          instantiateServiceProvider(serviceClassLoader.getClassLoader(), serviceDescriptor.getServiceProviderClassName());

      serviceProviders.add(serviceProvider);
    }
    return serviceProviders;
  }

  private ServiceProvider instantiateServiceProvider(ClassLoader classLoader, String className) throws ServiceResolutionError {
    Object reflectedObject;
    try {
      reflectedObject = withContextClassLoader(classLoader, () -> {
        try {
          return instanciateClass(className);
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
