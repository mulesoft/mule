/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
  public SchedulerService getServiceDefinition() {
    return new ServiceDefinition(SchedulerService.class, service);
  }

}
