/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.api.artifact;

import static org.mule.runtime.api.util.MuleSystemProperties.CLASSLOADER_SERVICE_JPMS_MODULE_LAYER;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;

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

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceClassLoaderFactoryProvider.class);

  public static ClassLoaderConfigurationLoader serviceClassLoaderConfigurationLoader() {
    return new LibFolderClassLoaderConfigurationLoader();
  }

  public static ServiceClassLoaderFactory serviceClassLoaderFactory() {
    if (useModuleLayer()) {
      LOGGER.debug("MRJAR 'ServiceClassLoaderFactoryProvider' implementation, using 'ServiceModuleLayerFactory'...");
      return new ServiceModuleLayerFactory();
    } else {
      LOGGER.debug("MRJAR 'ServiceClassLoaderFactoryProvider' implementation, using 'ServiceClassLoaderFactory'...");
      return new ServiceClassLoaderFactory();
    }
  }

  private static boolean useModuleLayer() {
    return parseBoolean(getProperty(CLASSLOADER_SERVICE_JPMS_MODULE_LAYER, "" + isJavaVersionAtLeast(JAVA_17)));
  }
}
