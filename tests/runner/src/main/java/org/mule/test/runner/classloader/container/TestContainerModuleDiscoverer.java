/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
