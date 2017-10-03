/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.manager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.internal.manager.MuleServiceManager;
import org.mule.runtime.module.service.api.discoverer.ServiceDiscoverer;
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
    final List<Pair<ArtifactClassLoader, Service>> services = new ArrayList<>();
    Pair<ArtifactClassLoader, Service> service1 = new Pair(mock(ArtifactClassLoader.class), mock(Service.class));
    Pair<ArtifactClassLoader, Service> service2 = new Pair(mock(ArtifactClassLoader.class), mock(Service.class));
    services.add(service1);
    services.add(service2);
    when(serviceDiscoverer.discoverServices()).thenReturn(services);

    final MuleServiceManager muleServiceManager = new MuleServiceManager(serviceDiscoverer);
    muleServiceManager.start();

    assertThat(muleServiceManager.getServices().size(), equalTo(2));
    assertThat(muleServiceManager.getServices().get(0), equalTo(service1.getSecond()));
    assertThat(muleServiceManager.getServices().get(1), equalTo(service2.getSecond()));
  }

  @Test
  public void startServices() throws Exception {
    final ServiceDiscoverer serviceDiscoverer = mock(ServiceDiscoverer.class);
    final List<Pair<ArtifactClassLoader, Service>> services = new ArrayList<>();
    Pair<ArtifactClassLoader, Service> service1 = new Pair(mock(ArtifactClassLoader.class), mock(StartableService.class));
    Pair<ArtifactClassLoader, Service> service2 = new Pair(mock(ArtifactClassLoader.class), mock(StartableService.class));
    services.add(service1);
    services.add(service2);
    when(serviceDiscoverer.discoverServices()).thenReturn(services);

    final MuleServiceManager muleServiceManager = new MuleServiceManager(serviceDiscoverer);
    muleServiceManager.start();

    InOrder inOrder = inOrder(service1.getSecond(), service2.getSecond());
    inOrder.verify((StartableService) service1.getSecond()).start();
    inOrder.verify((StartableService) service2.getSecond()).start();
  }

  @Test
  public void stopsServices() throws Exception {
    final ServiceDiscoverer serviceDiscoverer = mock(ServiceDiscoverer.class);
    final List<Pair<ArtifactClassLoader, Service>> services = new ArrayList<>();
    Pair<ArtifactClassLoader, Service> service1Pairs = new Pair(mock(ArtifactClassLoader.class), mock(StoppableService.class));
    Pair<ArtifactClassLoader, Service> service2Pairs = new Pair(mock(ArtifactClassLoader.class), mock(StoppableService.class));
    services.add(service1Pairs);
    services.add(service2Pairs);
    when(serviceDiscoverer.discoverServices()).thenReturn(services);

    final MuleServiceManager muleServiceManager = new MuleServiceManager(serviceDiscoverer);
    muleServiceManager.start();
    muleServiceManager.stop();

    InOrder inOrder = inOrder(service1Pairs.getSecond(), service2Pairs.getSecond());
    inOrder.verify((StoppableService) service2Pairs.getSecond()).stop();
    inOrder.verify((StoppableService) service1Pairs.getSecond()).stop();
  }

  @Test
  public void ignoresServiceStopError() throws Exception {
    final ServiceDiscoverer serviceDiscoverer = mock(ServiceDiscoverer.class);
    final List<Pair<ArtifactClassLoader, Service>> services = new ArrayList<>();
    StoppableService service1 = mock(StoppableService.class);
    doThrow(new RuntimeException()).when(service1).stop();
    StoppableService service2 = mock(StoppableService.class);
    doThrow(new RuntimeException()).when(service2).stop();
    services.add(new Pair<>(mock(ArtifactClassLoader.class), service1));
    services.add(new Pair<>(mock(ArtifactClassLoader.class), service2));
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
    final List<Pair<ArtifactClassLoader, Service>> services = new ArrayList<>();
    StoppableService service1 = mock(StoppableService.class);
    services.add(new Pair<>(mock(ArtifactClassLoader.class), service1));
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
