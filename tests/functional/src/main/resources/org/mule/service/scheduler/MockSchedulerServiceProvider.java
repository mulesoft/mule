/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler;

import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;

public class MockSchedulerServiceProvider implements ServiceProvider {

  private final SchedulerService service = new MockSchedulerService();

  @Override
  public ServiceDefinition getServiceDefinition() {
    return new ServiceDefinition(SchedulerService.class, service);
  }

}
