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
 * Has a factory method for obtaining a {@link ServiceClassLoaderFactory}.
 * 
 * @since 4.5
 * @deprecated since 4.8, use {@link ServiceClassLoaderFactoryProvider} instead.
 */
public class ServiceClassLoaderFactoryProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceClassLoaderFactoryProvider.class);

  public static ClassLoaderConfigurationLoader serviceClassLoaderConfigurationLoader() {
    return new LibFolderClassLoaderConfigurationLoader();
  }

  public static ServiceClassLoaderFactory serviceClassLoaderFactory() {
    LOGGER.debug("MRJAR 'ServiceClassLoaderFactoryProvider' implementation, using 'ServiceClassLoaderFactoryImpl'...");
    final ServiceClassLoaderFactory serviceModuleLayerFactory = new ServiceClassLoaderFactoryImpl();
    serviceModuleLayerFactory.setParentLayerFrom(ServiceClassLoaderFactoryProvider.class);
    return serviceModuleLayerFactory;
  }
}
