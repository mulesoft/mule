/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config.bootstrap;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.PropertiesUtils.discoverProperties;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * <p>
 * Looks for bootstrap properties in resources named META-INF/services/org/mule/config/registry-bootstrap.properties inside the
 * classpath.
 * </p>
 * <p>
 * All found properties resources are collected and loaded during the discovery process. Properties are returned in the same order
 * they were found in the classpath. If while loading some properties resource an exception occurs the whole process is
 * interrupted and a {@link org.mule.runtime.core.config.bootstrap.BootstrapException} exception is raised.
 * </p>
 */
public class ClassPathRegistryBootstrapDiscoverer implements RegistryBootstrapDiscoverer {

  public static final String BOOTSTRAP_PROPERTIES =
      "META-INF/services/org/mule/runtime/core/config/registry-bootstrap.properties";

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Properties> discover() throws BootstrapException {
    try {
      return discoverProperties(BOOTSTRAP_PROPERTIES);
    } catch (IOException e) {
      throw new BootstrapException(createStaticMessage("Could not load properties file"), e);
    }
  }
}
