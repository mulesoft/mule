/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.reboot.internal;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;

import static org.apache.logging.log4j.LogManager.setFactory;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.module.log4j.boot.api.MuleLog4jContextFactory;

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
      // We need to force the creation of a logger before we can change the manager factory.
      // This is because if not, any logger that will be acquired by MuleLog4jContextFactory code
      // will fail since it will try to use a null factory.
      getLogger("triggerDefaultFactoryCreation");
      // We need to set this property so log4j uses the same context factory everywhere
      setProperty("log4j2.loggerContextFactory", MuleLog4jContextFactory.class.getName());
      MuleLog4jContextFactory log4jContextFactory = new MuleLog4jContextFactory();
      setFactory(log4jContextFactory);
      System.out.println("Context factory configured");
    }
    return true;
  }
}
