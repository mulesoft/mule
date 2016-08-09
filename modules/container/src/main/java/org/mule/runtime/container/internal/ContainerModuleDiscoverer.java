/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.mule.runtime.core.util.Preconditions.checkArgument;

import java.util.List;

/**
 * Discovers modules on the Mule container.
 *
 * @since 4.0
 */
public class ContainerModuleDiscoverer implements ModuleDiscoverer {

  private final CompositeModuleDiscoverer moduleDiscoverer;

  /**
   * Creates a new instance.
   *
   * @param containerClassLoader container classloader used to find modules. Non null.
   */
  public ContainerModuleDiscoverer(ClassLoader containerClassLoader) {
    checkArgument(containerClassLoader != null, "containerClassLoader cannot be null");
    moduleDiscoverer =
        new CompositeModuleDiscoverer(new JreModuleDiscoverer(), new ClasspathModuleDiscoverer(containerClassLoader));
  }

  @Override
  public List<MuleModule> discover() {
    return moduleDiscoverer.discover();
  }
}
