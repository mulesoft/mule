/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Has a factory method for obtaining a {@link ModuleDiscoverer}.
 * <p>
 * This is to be overridden when running in a JVM that supports JPMS (9+).
 * 
 * @since 4.6
 */
public class ContainerModulesDiscovererProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContainerModulesDiscovererProvider.class);

  public static ModuleDiscoverer containerModulesDiscoverer() {
    LOGGER.debug("Default 'ContainerModulesDiscovererProvider' implementation, using 'ClasspathModuleDiscoverer'...");
    return new ClasspathModuleDiscoverer();
  }

}
