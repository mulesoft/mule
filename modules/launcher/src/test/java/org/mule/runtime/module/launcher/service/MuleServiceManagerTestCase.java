/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.InOrder;

public class MuleServiceManagerTestCase extends AbstractMuleTestCase {

  @Test
  public void registerServices() throws Exception {
    final ServiceDiscoverer serviceDiscoverer = mock(ServiceDiscoverer.class);
    final List<Service> services = new ArrayList<>();
    Service service1 = mock(Service.class);
    Service service2 = mock(Service.class);
    services.add(service1);
    services.add(service2);
    when(serviceDiscoverer.discoverServices()).thenReturn(services);

    final MuleServiceManager muleServiceManager = new MuleServiceManager(serviceDiscoverer);
    muleServiceManager.start();

    assertThat(muleServiceManager.getServices().size(), equalTo(2));
    assertThat(muleServiceManager.getServices().get(0), equalTo(service1));
    assertThat(muleServiceManager.getServices().get(1), equalTo(service2));
  }

  @Test
  public void startServices() throws Exception {
    final ServiceDiscoverer serviceDiscoverer = mock(ServiceDiscoverer.class);
    final List<Service> services = new ArrayList<>();
    StartableService service1 = mock(StartableService.class);
    StartableService service2 = mock(StartableService.class);
    services.add(service1);
    services.add(service2);
    when(serviceDiscoverer.discoverServices()).thenReturn(services);

    final MuleServiceManager muleServiceManager = new MuleServiceManager(serviceDiscoverer);
    muleServiceManager.start();

    InOrder inOrder = inOrder(service1, service2);
    inOrder.verify(service1).start();
    inOrder.verify(service2).start();
  }

  @Test
  public void stopsServices() throws Exception {
    final ServiceDiscoverer serviceDiscoverer = mock(ServiceDiscoverer.class);
    final List<Service> services = new ArrayList<>();
    StoppableService service1 = mock(StoppableService.class);
    StoppableService service2 = mock(StoppableService.class);
    services.add(service1);
    services.add(service2);
    when(serviceDiscoverer.discoverServices()).thenReturn(services);

    final MuleServiceManager muleServiceManager = new MuleServiceManager(serviceDiscoverer);
    muleServiceManager.start();
    muleServiceManager.stop();

    InOrder inOrder = inOrder(service1, service2);
    inOrder.verify(service2).stop();
    inOrder.verify(service1).stop();
  }

  @Test
  public void ignoresServiceStopError() throws Exception {
    final ServiceDiscoverer serviceDiscoverer = mock(ServiceDiscoverer.class);
    final List<Service> services = new ArrayList<>();
    StoppableService service1 = mock(StoppableService.class);
    doThrow(new RuntimeException()).when(service1).stop();
    StoppableService service2 = mock(StoppableService.class);
    doThrow(new RuntimeException()).when(service2).stop();
    services.add(service1);
    services.add(service2);
    when(serviceDiscoverer.discoverServices()).thenReturn(services);

    final MuleServiceManager muleServiceManager = new MuleServiceManager(serviceDiscoverer);
    muleServiceManager.start();
    muleServiceManager.stop();

    InOrder inOrder = inOrder(service1, service2);
    inOrder.verify(service2).stop();
    inOrder.verify(service1).stop();
  }

  @Test
  public void wrapsServices() throws Exception {
    final ServiceDiscoverer serviceDiscoverer = mock(ServiceDiscoverer.class);
    final List<Service> services = new ArrayList<>();
    StoppableService service1 = mock(StoppableService.class);
    services.add(service1);
    when(serviceDiscoverer.discoverServices()).thenReturn(services);

    final MuleServiceManager muleServiceManager = new MuleServiceManager(serviceDiscoverer);
    muleServiceManager.start();

    assertThat(Proxy.isProxyClass(muleServiceManager.getServices().get(0).getClass()), is(true));
  }

  public interface StartableService extends Service, Startable {

  }

  public interface StoppableService extends Service, Stoppable {

  }
}
