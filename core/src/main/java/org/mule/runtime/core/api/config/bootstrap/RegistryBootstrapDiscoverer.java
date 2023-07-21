/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.config.bootstrap;

import org.mule.api.annotation.NoImplement;

import java.util.List;
import java.util.Properties;

/**
 * Allows to discover properties to be used during the bootstrap process. Implementing this interface you will be able to
 * customize which Properties are taken into account at the bootstrap process, and the order they are processed.
 */
@NoImplement
public interface RegistryBootstrapDiscoverer {

  /**
   * Collects the Properties to be used in an ordered way.
   *
   * @return A list of Properties containing the key/value pairs to be used in the bootstrap configuration process.
   * @throws BootstrapException if a problem occurs during the discovery process.
   */
  List<Properties> discover() throws BootstrapException;
}
