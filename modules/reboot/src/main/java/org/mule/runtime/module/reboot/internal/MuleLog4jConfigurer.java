/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.reboot.internal;

import static org.mule.runtime.module.log4j.boot.api.MuleLog4jContextFactory.createAndInstall;

import static java.lang.System.getProperty;

/**
 * A {@link BootstrapConfigurer} that configures the log4j factory.
 *
 * @since 4.5
 */
public class MuleLog4jConfigurer implements BootstrapConfigurer {

  public static final String MULE_SIMPLE_LOG = "mule.simpleLog";

  @Override
  public boolean configure() throws BootstrapConfigurationException {
    if (getProperty(MULE_SIMPLE_LOG) == null) {
      createAndInstall();
    }
    return true;
  }
}
