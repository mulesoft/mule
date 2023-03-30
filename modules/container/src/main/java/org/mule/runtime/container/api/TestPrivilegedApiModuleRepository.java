/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api;

import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.TestModuleDiscoverer;

import java.util.List;
import java.util.Set;

/**
 * A {@link ModuleRepository} that enables to change discovered modules to enable testing privileged API scenarios.
 *
 * @since 4.6
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
