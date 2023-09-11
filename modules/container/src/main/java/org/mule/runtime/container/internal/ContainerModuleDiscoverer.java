/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import org.mule.runtime.jpms.api.MuleContainerModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Discovers modules on the Mule container.
 *
 * @since 4.0
 */
public final class ContainerModuleDiscoverer implements ModuleDiscoverer {

  private final List<ModuleDiscoverer> moduleDiscoverers;

  /**
   * Creates a new instance.
   *
   * @param containerClassLoader container classloader used to find modules. Non null.
   */
  public ContainerModuleDiscoverer() {
    this.moduleDiscoverers = getModuleDiscoverers();
  }

  private List<ModuleDiscoverer> getModuleDiscoverers() {
    List<ModuleDiscoverer> result = new ArrayList<>();
    result.add(new JreModuleDiscoverer());
    result.add(new ClasspathModuleDiscoverer());
    return result;
  }

  public void addModuleDiscoverer(ModuleDiscoverer moduleDiscoverer) {
    this.moduleDiscoverers.add(moduleDiscoverer);
  }

  @Override
  public List<MuleContainerModule> discover() {
    return new CompositeModuleDiscoverer(this.moduleDiscoverers.toArray(new ModuleDiscoverer[0])).discover();
  }
}
