/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
