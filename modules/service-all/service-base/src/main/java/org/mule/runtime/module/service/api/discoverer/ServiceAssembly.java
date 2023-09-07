/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.discoverer;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;

/**
 * Assembles a {@link Service} implemented through a service artifact providing all the pieces necessary to use it
 *
 * @since 4.2
 */
@NoImplement
public interface ServiceAssembly {

  /**
   * @return The service name
   */
  String getName();

  /**
   * @return The {@link ServiceProvider} through which the service can be instantiated
   */
  ServiceProvider getServiceProvider();

  /**
   * @return the service's {@link ClassLoader}
   */
  ClassLoader getClassLoader();

  /**
   * @return The contract interface that is being fulfilled
   */
  Class<? extends Service> getServiceContract();

}
