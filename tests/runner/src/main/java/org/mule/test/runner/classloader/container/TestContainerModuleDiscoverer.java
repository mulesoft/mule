/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.classloader.container;

import org.mule.runtime.container.internal.ClasspathTestModuleDiscoverer;
import org.mule.runtime.container.internal.ContainerModuleDiscoverer;
import org.mule.runtime.container.internal.ModuleDiscoverer;

import java.util.List;

/**
 * Discovers container modules including both productive and test modules
 *
 * @since 4.5
 */
public class TestContainerModuleDiscoverer extends ContainerModuleDiscoverer {

  /**
   * Creates a new instance.
   *
   * @param containerClassLoader container classloader used to find modules. Non null.
   */
  public TestContainerModuleDiscoverer(ClassLoader containerClassLoader) {
    super(containerClassLoader);
  }

  @Override
  protected List<ModuleDiscoverer> getModuleDiscoverers(ClassLoader containerClassLoader) {
    List<ModuleDiscoverer> result = super.getModuleDiscoverers(containerClassLoader);
    result.add(new ClasspathTestModuleDiscoverer(containerClassLoader));
    return result;
  }
}
