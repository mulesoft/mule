/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler;

import static java.util.Collections.singletonList;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerView;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerPoolsConfigFactory;
import org.mule.runtime.api.scheduler.SchedulerService;

import java.util.List;

public class MockSchedulerService implements SchedulerService {

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Scheduler cpuLightScheduler() {
    return new MockScheduler();
  }

  @Override
  public Scheduler ioScheduler() {
    return new MockScheduler();
  }

  @Override
  public Scheduler cpuIntensiveScheduler() {
    return new MockScheduler();
  }
  
  @Override
  public Scheduler cpuLightScheduler(SchedulerConfig config) {
    return new MockScheduler();
  }

  @Override
  public Scheduler ioScheduler(SchedulerConfig config) {
    return new MockScheduler();
  }

  @Override
  public Scheduler cpuIntensiveScheduler(SchedulerConfig config) {
    return new MockScheduler();
  }
  
  @Override
  public Scheduler cpuLightScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
    return new MockScheduler();
  }
  
  @Override
  public Scheduler ioScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
    return new MockScheduler();
  }
  
  @Override
  public Scheduler cpuIntensiveScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
    return new MockScheduler();
  }
  
  @Override
  public Scheduler customScheduler(SchedulerConfig config) {
    return new MockScheduler();
  }
  
  @Override
  public Scheduler customScheduler(SchedulerConfig config, int queueSize) {
    return new MockScheduler();
  }

  @Override
  public List<SchedulerView> getSchedulers() {
    return singletonList(new MockSchedulerView());
  }
}
