/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.artifact;

import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.service.internal.artifact.LibFolderClassLoaderConfigurationLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Has a factory method for obtaining a {@link IServiceClassLoaderFactory}.
 *
 * @since 4.5
 */
public class ServiceClassLoaderFactoryProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceClassLoaderFactoryProvider.class);

  public static ClassLoaderConfigurationLoader serviceClassLoaderConfigurationLoader() {
    return new LibFolderClassLoaderConfigurationLoader();
  }

  public static IServiceClassLoaderFactory serviceClassLoaderFactory() {
    LOGGER.debug("MRJAR 'ServiceClassLoaderFactoryProvider' implementation, using 'ServiceModuleLayerFactory'...");
    final ServiceModuleLayerFactory serviceModuleLayerFactory = new ServiceModuleLayerFactory();
    serviceModuleLayerFactory.setParentLayerFrom(ServiceClassLoaderFactoryProvider.class);
    return serviceModuleLayerFactory;
  }
}
