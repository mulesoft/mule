/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
    return new MockScheduler(config.getSchedulerName());
  }

  @Override
  public Scheduler ioScheduler(SchedulerConfig config) {
    return new MockScheduler(config.getSchedulerName());
  }

  @Override
  public Scheduler cpuIntensiveScheduler(SchedulerConfig config) {
    return new MockScheduler(config.getSchedulerName());
  }
  
  @Override
  public Scheduler cpuLightScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
    return new MockScheduler(config.getSchedulerName());
  }
  
  @Override
  public Scheduler ioScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
    return new MockScheduler(config.getSchedulerName());
  }
  
  @Override
  public Scheduler cpuIntensiveScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
    return new MockScheduler(config.getSchedulerName());
  }
  
  @Override
  public Scheduler customScheduler(SchedulerConfig config) {
    return new MockScheduler(config.getSchedulerName());
  }
  
  @Override
  public Scheduler customScheduler(SchedulerConfig config, int queueSize) {
    return new MockScheduler(config.getSchedulerName());
  }

  @Override
  public List<SchedulerView> getSchedulers() {
    return singletonList(new MockSchedulerView());
  }
}
