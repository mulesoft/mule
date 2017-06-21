/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.PropertiesUtils.discoverProperties;

import org.mule.runtime.core.api.config.bootstrap.BootstrapException;
import org.mule.runtime.core.api.config.bootstrap.RegistryBootstrapDiscoverer;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * <p>
 * Looks for bootstrap properties in resources named {code BOOTSTRAP_PROPERTIES} inside a given classloader.
 * </p>
 * <p>
 * All found properties resources are collected and loaded during the discovery process. Properties are returned in the same order
 * they were found in the classloader. If while loading some properties resource an exception occurs the whole process is
 * interrupted and a {@link BootstrapException} exception is raised.
 * </p>
 */
public class ClassLoaderRegistryBootstrapDiscoverer implements RegistryBootstrapDiscoverer {

  public static final String BOOTSTRAP_PROPERTIES =
      "META-INF/org/mule/runtime/core/config/registry-bootstrap.properties";
  private final ClassLoader classLoader;

  /**
   * Creates a new discoverer for a given classloader.
   *
   * @param classLoader classloader used to discover {code BOOTSTRAP_PROPERTIES} files. Non null.
   */
  public ClassLoaderRegistryBootstrapDiscoverer(ClassLoader classLoader) {
    checkArgument(classLoader != null, "Classloader cannot be null");
    this.classLoader = classLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Properties> discover() throws BootstrapException {
    try {
      return discoverProperties(classLoader, BOOTSTRAP_PROPERTIES);
    } catch (IOException e) {
      throw new BootstrapException(createStaticMessage("Could not load properties file"), e);
    }
  }
}
