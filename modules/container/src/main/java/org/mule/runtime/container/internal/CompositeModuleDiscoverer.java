/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.container.api.MuleModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Composes a group of {@link ModuleDiscoverer} and discovers the modules discovered by each of them.
 *
 * @since 4.0
 */
public class CompositeModuleDiscoverer implements ModuleDiscoverer {

  private final ModuleDiscoverer[] moduleDiscoverers;

  /**
   * Creates a new instance.
   *
   * @param moduleDiscoverers module discoveres to compose. Non empty.
   */
  public CompositeModuleDiscoverer(ModuleDiscoverer... moduleDiscoverers) {
    checkArgument(moduleDiscoverers.length > 0, "moduleDiscoverers cannot be empty");
    this.moduleDiscoverers = moduleDiscoverers;
  }

  @Override
  public List<MuleModule> discover() {
    final List<MuleModule> muleModules = new ArrayList<>();
    for (ModuleDiscoverer discoverer : moduleDiscoverers) {
      muleModules.addAll(discoverer.discover());
    }

    return muleModules;
  }
}
