/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.config.bootstrap;

import org.mule.api.annotation.NoImplement;

import java.util.List;

/**
 * Discovers available {@link BootstrapService} instances
 */
@NoImplement
public interface BootstrapServiceDiscoverer {

  /**
   * Discovers all the services available on the execution context.
   *
   * @return a non null list of {@link BootstrapService}
   */
  List<BootstrapService> discover();

}
