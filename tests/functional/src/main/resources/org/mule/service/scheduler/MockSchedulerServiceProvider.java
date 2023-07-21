/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
