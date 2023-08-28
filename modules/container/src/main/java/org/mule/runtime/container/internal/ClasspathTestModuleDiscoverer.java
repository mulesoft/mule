/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.container.internal;

import org.mule.runtime.container.api.MuleModule;

/**
 * Discovers {@link MuleModule} searching for {@link #MODULE_PROPERTIES} and {@link #TEST_MODULE_PROPERTIES} files resources
 * available in a given classloader.
 *
 * @since 4.5
 */
public class ClasspathTestModuleDiscoverer extends ClasspathModuleDiscoverer {

  private static final String TEST_MODULE_PROPERTIES = "META-INF/mule-test-module.properties";

  public ClasspathTestModuleDiscoverer(ClassLoader classLoader) {
    super(classLoader);
  }

  @Override
  protected String getModulePropertiesFileName() {
    return TEST_MODULE_PROPERTIES;
  }
}
