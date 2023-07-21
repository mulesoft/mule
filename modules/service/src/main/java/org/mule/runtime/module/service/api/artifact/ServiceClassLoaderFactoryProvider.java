/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.api.artifact;

import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.service.internal.artifact.LibFolderClassLoaderConfigurationLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Has a factory method for obtaining a {@link ServiceClassLoaderFactory}.
 * <p>
 * This is to be overridden when running in a JVM that supports JPMS (9+).
 * 
 * @since 4.5
 */
public class ServiceClassLoaderFactoryProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceClassLoaderFactoryProvider.class);

  public static ClassLoaderConfigurationLoader serviceClassLoaderConfigurationLoader() {
    return new LibFolderClassLoaderConfigurationLoader();
  }

  public static ServiceClassLoaderFactory serviceClassLoaderFactory() {
    LOGGER.debug("Default 'ServiceClassLoaderFactoryProvider' implementation, using 'ServiceClassLoaderFactory'...");
    return new ServiceClassLoaderFactory();
  }
}
