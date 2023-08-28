/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.launcher.coreextension;

import org.mule.runtime.container.api.MuleCoreExtension;

import java.util.List;

/**
 * Discovers dependencies between {@link MuleCoreExtension} instances
 */
public interface MuleCoreExtensionDependencyDiscoverer {

  /**
   * Finds dependencies defined in a given {@link MuleCoreExtension}
   *
   * @param coreExtension dependant core extension
   * @return a not null list of dependencies found in the extension class
   */
  List<LinkedMuleCoreExtensionDependency> findDependencies(MuleCoreExtension coreExtension);
}
