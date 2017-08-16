/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

public class ServiceLifecycleWrapper implements Service, Stoppable, Startable {

  private final ArtifactClassLoader serviceClassLoader;
  private Service service;

  public ServiceLifecycleWrapper(ArtifactClassLoader serviceClassLoader, Service service) {
    this.service = service;
    this.serviceClassLoader = serviceClassLoader;
  }

  @Override
  public String getName() {
    return service.getName();
  }

  @Override
  public void stop() throws MuleException {
    if (service instanceof Stoppable) {
      ((Stoppable) service).stop();
    }
    serviceClassLoader.dispose();
  }

  @Override
  public void start() throws MuleException {
    if (service instanceof Startable) {
      ((Startable) service).start();
    }
  }

}
