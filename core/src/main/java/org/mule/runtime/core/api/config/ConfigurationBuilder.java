/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.core.api.MuleContext;

/**
 * A <code>ConfigurationBuilder</code> is used to configure a Mule instance, represented by a MuleContext instance. Multiple
 * ConfigurationBuilder's can be used to configure a single mule instance with each ConfigurationBuilder doing one of more of the
 * following:
 * <li>Creation of mule runtime artifacts (endpoint's, connector's, service's, transformer's) which are then registered with the
 * <code>Registry</code
 * <li> Creation and registration of SecurityManager / TransactionManager / TransactionManagerFactory / QueueManager
 * and ThreadingProfile's.  Unlike the runtime artifacts mule only uses a single instance of each of these and so if
 * multiple configuration builder create and register these artifacts only one will be used.
 * <li> Configuration of existing Mule configuration related artifacts such as <code>MuleConfiguration</code> and
 * <code>ServerNotificationManager</code> <br/>
 * <br/>
 * Which of the above takes place, depends on what the configuration source contains and the ConfgurationBuilder implementation is
 * used.
 */
public interface ConfigurationBuilder {

  /**
   * Adds a service configurator to be used on the context being built.
   *
   * @param serviceConfigurator service to add. Non null.
   */
  void addServiceConfigurator(ServiceConfigurator serviceConfigurator);

  /**
   * Will configure a MuleContext based on the configuration provided. The configuration will be set on the
   * {@link org.mule.runtime.core.api.config.ConfigurationBuilder} implementation as bean properties before this method has been
   * called.
   *
   * @param muleContext The current {@link org.mule.runtime.core.api.MuleContext}
   * @throws ConfigurationException if the configuration fails i.e. an object cannot be created or initialised properly
   */
  void configure(MuleContext muleContext) throws ConfigurationException;
}
