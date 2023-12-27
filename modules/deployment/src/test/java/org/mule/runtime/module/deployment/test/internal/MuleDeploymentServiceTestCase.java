/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static java.lang.Thread.MIN_PRIORITY;
import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.internal.MuleDeploymentService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class MuleDeploymentServiceTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Rule
  public TemporaryFolder artifactsDir = new TemporaryFolder();

  private static final int NUMBER_OF_LISTENER_SUBSCRIBERS = 0;

  @Mock
  DefaultDomainFactory domainFactory;

  @Mock
  DefaultApplicationFactory applicationFactory;

  @Mock
  SchedulerService schedulerService;

  Supplier<SchedulerService> schedulerServiceSupplier = () -> schedulerService;

  @Test
  public void startupListenersAddDoesNotResultInConcurrentException() throws Exception {
    MuleDeploymentService deploymentService =
        new MuleDeploymentService(domainFactory, applicationFactory, schedulerServiceSupplier);
    List<Thread> listenersSubscribers = new ArrayList<>();

    for (int i = 0; i < NUMBER_OF_LISTENER_SUBSCRIBERS; i++) {
      listenersSubscribers.add(new Thread(new ListenerSubscriber(deploymentService)));
    }

    for (Thread listenerSubscriber : listenersSubscribers) {
      listenerSubscriber.start();
    }

    deploymentService.notifyStartupListeners();

    for (Thread listenerSubscriber : listenersSubscribers) {
      listenerSubscriber.join();
    }
  }

  @Test
  public void deploymentMonitorUsesSchedulerServiceWithMinPriority() {
    // Minimal mocking so MuleDeploymentService#start succeeds
    Scheduler mockScheduler = mock(Scheduler.class);
    when(schedulerService.customScheduler(any())).thenReturn(mockScheduler);
    when(domainFactory.getArtifactDir()).thenReturn(artifactsDir.getRoot());
    when(applicationFactory.getArtifactDir()).thenReturn(artifactsDir.getRoot());

    MuleDeploymentService deploymentService =
        new MuleDeploymentService(domainFactory, applicationFactory, schedulerServiceSupplier);

    // This triggers the periodic scheduling of the DeploymentDirectoryWatcher
    deploymentService.start();
    deploymentService.stop();

    ArgumentCaptor<SchedulerConfig> schedulerConfigCaptor = forClass(SchedulerConfig.class);
    verify(schedulerService).customScheduler(schedulerConfigCaptor.capture());
    assertThat(schedulerConfigCaptor.getValue().getPriority(), is(of(MIN_PRIORITY)));
  }

  private static class ListenerSubscriber implements Runnable {

    private final MuleDeploymentService deploymentService;

    public ListenerSubscriber(MuleDeploymentService deploymentService) {
      this.deploymentService = deploymentService;
    }

    @Override
    public void run() {
      deploymentService.addStartupListener(() -> {
        // Do nothing
      });
    }
  }

}
