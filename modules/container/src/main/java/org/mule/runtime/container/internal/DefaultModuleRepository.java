/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;

import java.util.List;

/**
 * Defines a {@link ModuleRepository} that uses a {@link ModuleDiscoverer} to find the available
 * modules.
 */
public class DefaultModuleRepository implements ModuleRepository {

  private final ModuleDiscoverer moduleDiscoverer;
  private List<MuleModule> modules;

  /**
   * Creates a new repository
   *
   * @param moduleDiscoverer used to discover available modules. Non null.
   */
  public DefaultModuleRepository(ModuleDiscoverer moduleDiscoverer) {
    checkArgument(moduleDiscoverer != null, "moduleDiscoverer cannot be null");

    this.moduleDiscoverer = moduleDiscoverer;
  }

  @Override
  public List<MuleModule> getModules() {
    if (modules == null) {
      synchronized (this) {
        if (modules == null) {
          modules = discoverModules();
        }
      }
    }

    return modules;
  }

  protected List<MuleModule> discoverModules() {
    return moduleDiscoverer.discover();
  }
}
