/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.bootstrap;

import java.util.List;
import java.util.Properties;

/**
 * Allows to discover properties to be used during the bootstrap process. Implementing this interface you will be able to
 * customize which Properties are taken into account at the bootstrap process, and the order they are processed.
 */
public interface RegistryBootstrapDiscoverer {

  /**
   * Collects the Properties to be used in an ordered way.
   *
   * @return A list of Properties containing the key/value pairs to be used in the bootstrap configuration process.
   * @throws BootstrapException if a problem occurs during the discovery process.
   */
  List<Properties> discover() throws BootstrapException;
}
