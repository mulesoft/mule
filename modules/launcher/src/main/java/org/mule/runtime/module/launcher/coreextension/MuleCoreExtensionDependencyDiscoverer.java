/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
