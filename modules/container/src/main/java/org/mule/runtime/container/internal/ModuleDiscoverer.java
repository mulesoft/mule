/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import org.mule.runtime.jpms.api.MuleContainerModule;

import java.util.List;

/**
 * Discovers available mule modules.
 */
public interface ModuleDiscoverer {

  /**
   * Discovers available mule modules.
   *
   * @return a non null {@link List} containing all {@link MuleContainerModule} found in the container.
   */
  List<MuleContainerModule> discover();
}
