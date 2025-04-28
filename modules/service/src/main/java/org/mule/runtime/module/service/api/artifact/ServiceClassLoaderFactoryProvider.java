/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.artifact;

import static org.mule.runtime.api.util.MuleSystemProperties.classloaderContainerJpmsModuleLayer;

import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.service.internal.artifact.LibFolderClassLoaderConfigurationLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Has a factory method for obtaining a {@link ServiceClassLoaderFactory}.
 * <p>
 * This overrides the default class when running in a JVM that supports JPMS (9+), but has a fallback mechanism to provide the
 * previous implementation.
 *
 * @since 4.5
 */
public class ServiceClassLoaderFactoryProvider {

  private ServiceClassLoaderFactoryProvider() {}

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceClassLoaderFactoryProvider.class);

  public static ClassLoaderConfigurationLoader serviceClassLoaderConfigurationLoader() {
    return new LibFolderClassLoaderConfigurationLoader();
  }

  private static boolean withinModularizedContainer = ServiceClassLoaderFactoryProvider.class.getModule().isNamed();

  @Deprecated(since = "4.9")
  public static ServiceClassLoaderFactory serviceClassLoaderFactory() {
    if (classloaderContainerJpmsModuleLayer()
        // Only if the container is modularized it makes sense to load services as module layers
        && withinModularizedContainer) {
      LOGGER.debug("MRJAR 'ServiceClassLoaderFactoryProvider' implementation, using 'ServiceModuleLayerFactory'...");
      final ServiceModuleLayerFactory serviceModuleLayerFactory = new ServiceModuleLayerFactory();
      serviceModuleLayerFactory.setParentLayerFrom(ServiceClassLoaderFactoryProvider.class);
      return serviceModuleLayerFactory;
    } else {
      LOGGER.debug("MRJAR 'ServiceClassLoaderFactoryProvider' implementation, using 'ServiceClassLoaderFactory'...");
      return new ServiceClassLoaderFactory();
    }
  }

  public static void setWithinModularizedContainer(boolean withinModularizedContainer) {
    ServiceClassLoaderFactoryProvider.withinModularizedContainer = withinModularizedContainer;
  }
}
