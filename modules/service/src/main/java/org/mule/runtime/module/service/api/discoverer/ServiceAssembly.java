/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
