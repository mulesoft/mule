/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.commons.internal;

import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * A {@link BootstrapConfigurer} that takes care of installing the bridge/router for all JUL log records to the SLF4J API.
 *
 * @since 4.6
 */
public class SLF4JBridgeHandlerBootstrapConfigurer implements BootstrapConfigurer {

  public boolean configure() throws BootstrapConfigurationException {
    try {
      // Optionally remove existing handlers attached to j.u.l root logger
      SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)

      // add SLF4JBridgeHandler to j.u.l's root logger
      SLF4JBridgeHandler.install();
      return true;
    } catch (Exception e) {
      throw new BootstrapConfigurationException(1, e);
    }
  }
}
