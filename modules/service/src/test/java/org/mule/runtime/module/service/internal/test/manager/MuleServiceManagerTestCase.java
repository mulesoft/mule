/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.internal.test.manager;

import static java.util.Arrays.asList;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.service.api.discoverer.ServiceDiscoverer;
import org.mule.runtime.module.service.internal.manager.MuleServiceManager;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class MuleServiceManagerTestCase extends AbstractMuleTestCase {

  private Service service1;
  private Service service2;

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Mock
  private ServiceDiscoverer serviceDiscoverer;

  private MuleServiceManager muleServiceManager;

  @Before
  public void before() throws Exception {
    service1 = mockService();
    service2 = mockService();

    when(serviceDiscoverer.discoverServices()).thenReturn(asList(service1, service2));
    when(service1.getName()).thenReturn("Awesome Service");
    when(service2.getName()).thenReturn("Yet another awesome Service");

    muleServiceManager = new MuleServiceManager(serviceDiscoverer);
  }

  private Service mockService() {
    return mock(Service.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
  }

  @Test
  public void registerServices() throws Exception {
    muleServiceManager.start();

    assertThat(muleServiceManager.getServices(), hasSize(2));
    assertThat(muleServiceManager.getServices().get(0), is(service1));
    assertThat(muleServiceManager.getServices().get(1), is(service2));
  }

  @Test
  public void startServices() throws Exception {
    muleServiceManager.start();

    InOrder inOrder = inOrder(service1, service2);
    inOrder.verify((Startable) service1).start();
    inOrder.verify((Startable) service2).start();
  }

  @Test
  public void stopsServices() throws Exception {
    muleServiceManager.start();
    muleServiceManager.stop();

    InOrder inOrder = inOrder(service1, service2);
    inOrder.verify((Stoppable) service1).stop();
    inOrder.verify((Stoppable) service2).stop();
  }

  @Test
  public void ignoresServiceStopError() throws Exception {
    doThrow(new RuntimeException()).when((Stoppable) service1).stop();
    muleServiceManager.start();
    muleServiceManager.stop();

    InOrder inOrder = inOrder(service1, service2);
    inOrder.verify((Stoppable) service1).stop();
    inOrder.verify((Stoppable) service2).stop();
  }

  @Test
  public void httpAndSchedulerServicesAreStoppedInOrder() throws Exception {
    Service mockSchedulerService = mockService();
    when(mockSchedulerService.getName()).thenReturn("Scheduler service");
    Service mockHttpService = mockService();
    when(mockHttpService.getName()).thenReturn("HTTP Service");

    when(serviceDiscoverer.discoverServices()).thenReturn(Arrays.asList(
                                                                        service1,
                                                                        mockSchedulerService,
                                                                        service2,
                                                                        mockHttpService));

    muleServiceManager.start();
    muleServiceManager.stop();

    InOrder inOrder = inOrder(mockHttpService, mockSchedulerService);
    inOrder.verify((Stoppable) mockHttpService).stop();
    inOrder.verify((Stoppable) mockSchedulerService).stop();
  }
}
