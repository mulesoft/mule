/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.jpms.api.MuleContainerModule;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a {@link ModuleRepository} that uses a {@link ModuleDiscoverer} to find the available modules.
 */
public class DefaultModuleRepository implements ModuleRepository {

  protected static final Logger logger = LoggerFactory.getLogger(DefaultModuleRepository.class);

  private final ModuleDiscoverer moduleDiscoverer;
  private volatile List<MuleContainerModule> modules;

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
  public List<MuleContainerModule> getModules() {
    if (modules == null) {
      synchronized (this) {
        if (modules == null) {
          modules = discoverModules();

          if (logger.isDebugEnabled()) {
            logger.debug("Found {} modules: {}", modules.size(), modules.stream().map(m -> m.getName()).collect(toList()));
          }
        }
      }
    }

    return modules;
  }

  protected List<MuleContainerModule> discoverModules() {
    return moduleDiscoverer.discover();
  }
}
