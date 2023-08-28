/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.container.internal;

import org.mule.runtime.container.api.MuleModule;

import java.util.List;

/**
 * Discovers available mule modules.
 */
public interface ModuleDiscoverer {

  /**
   * Discovers available mule modules.
   *
   * @return a non null {@link List} containing all {@link MuleModule} found in the container.
   */
  List<MuleModule> discover();
}
