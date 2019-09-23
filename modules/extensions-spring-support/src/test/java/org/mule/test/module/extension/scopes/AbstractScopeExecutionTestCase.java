/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.scopes;

import static java.lang.Runtime.getRuntime;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class AbstractScopeExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  protected static final String KILL_REASON = "I'm the one who knocks";

  @Inject
  protected SchedulerService schedulerService;

  protected Scheduler cpuLightScheduler;
  protected Scheduler testScheduler;

  @Rule
  public SystemProperty maxRedelivery = new SystemProperty("killingReason", KILL_REASON);

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"scopes/heisenberg-scope-config.xml"};
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Before
  public void setUp() {
    cpuLightScheduler = schedulerService.cpuLightScheduler();
    testScheduler = schedulerService.customScheduler(SchedulerConfig.config().withName("SCOPE-TEST")
        .withMaxConcurrentTasks(2 + getRuntime().availableProcessors() * 2));
  }

  @After
  public void tearDown() {
    cpuLightScheduler.stop();
    testScheduler.stop();
  }
}
