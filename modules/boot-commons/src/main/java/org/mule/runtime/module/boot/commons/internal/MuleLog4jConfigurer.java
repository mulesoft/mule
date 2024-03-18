/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.commons.internal;


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
