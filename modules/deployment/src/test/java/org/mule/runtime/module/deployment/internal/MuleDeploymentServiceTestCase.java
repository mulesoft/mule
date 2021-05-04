/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;

@RunWith(MockitoJUnitRunner.class)
public class MuleDeploymentServiceTestCase {

  private static final int NUMBER_OF_LISTENER_SUBSCRIBERS = 0;

  @Mock
  DefaultDomainFactory domainFactory;

  @Mock
  DefaultApplicationFactory applicationFactory;

  @Mock
  Supplier<SchedulerService> schedulerServiceSupplier;

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

  private static class ListenerSubscriber implements Runnable {

    private MuleDeploymentService deploymentService;

    public ListenerSubscriber(MuleDeploymentService deploymentService) {
      this.deploymentService = deploymentService;
    }

    @Override
    public void run() {
      deploymentService.addStartupListener(new StartupListener() {

        @Override
        public void onAfterStartup() {
          // Do nothing
        }
      });
    }
  }

}
