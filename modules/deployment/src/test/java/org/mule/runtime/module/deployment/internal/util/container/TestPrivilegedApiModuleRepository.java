/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal.util.container;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.test.runner.classloader.container.TestModuleDiscoverer;

import java.util.List;
import java.util.Set;

/**
 * A {@link ModuleRepository} that enables to change discovered modules to enable testing privileged API scenarios.
 *
 * @since 4.5
 */
public final class TestPrivilegedApiModuleRepository implements ModuleRepository {

  private final DefaultModuleRepository moduleRepository;

  public TestPrivilegedApiModuleRepository(Set<String> privligedArtifactIds) {
    moduleRepository = new DefaultModuleRepository(new TestModuleDiscoverer(privligedArtifactIds));
  }

  @Override
  public List<MuleModule> getModules() {
    return moduleRepository.getModules();
  }
}
