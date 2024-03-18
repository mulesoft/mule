/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
