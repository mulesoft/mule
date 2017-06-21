/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.config.bootstrap;

import java.util.List;

/**
 * Discovers available {@link BootstrapService} instances
 */
public interface BootstrapServiceDiscoverer {

  /**
   * Discovers all the services available on the execution context.
   *
   * @return a non null list of {@link BootstrapService}
   */
  List<BootstrapService> discover();

}
