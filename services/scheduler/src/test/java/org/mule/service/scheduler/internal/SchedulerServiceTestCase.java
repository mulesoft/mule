/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SCHEDULER_SERVICE;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.scheduler.SchedulerPoolsConfig;
import org.mule.runtime.core.api.scheduler.SchedulerPoolsConfigFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Features;

@Features(SCHEDULER_SERVICE)
public class SchedulerServiceTestCase extends AbstractMuleTestCase {

  private DefaultSchedulerService service;

  @Before
  public void before() throws MuleException {
    service = new DefaultSchedulerService();
    service.start();
  }

  @After
  public void after() throws MuleException {
    service.stop();
  }

  @Test
  public void defaultNoConfig() {
    assertThat(service.getPools(), hasSize(1));
    service.cpuLightScheduler();
    assertThat(service.getPools(), hasSize(1));
    service.cpuLightScheduler();
    assertThat(service.getPools(), hasSize(1));
  }

  @Test
  public void artifactConfig() {
    assertThat(service.getPools(), hasSize(1));

    final SchedulerPoolsConfigFactory configFactory = getMockConfigFactory();
    service.cpuLightScheduler(config(), configFactory);
    assertThat(service.getPools(), hasSize(2));
    service.cpuLightScheduler(config(), configFactory);
    assertThat(service.getPools(), hasSize(2));
  }

  @Test
  public void addWithArtifactConfig() {
    assertThat(service.getPools(), hasSize(1));
    service.cpuLightScheduler();
    assertThat(service.getPools(), hasSize(1));
    service.cpuLightScheduler(config(), getMockConfigFactory());
    assertThat(service.getPools(), hasSize(2));
  }

  @Test
  public void artifactGarbageCollectedConfig() {
    assertThat(service.getPools(), hasSize(1));

    // We cannot use Mockito to create this object, beacuase Mockito keeps hard reference to the mocks it creares.
    SchedulerPoolsConfigFactory config = () -> of(getMockConfig());
    PhantomReference<SchedulerPoolsConfigFactory> configFactoryRef = new PhantomReference<>(config, new ReferenceQueue<>());

    service.cpuLightScheduler(config(), config);
    assertThat(service.getPools(), hasSize(2));

    config = null;

    new PollingProber(10000, 500).check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(configFactoryRef.isEnqueued(), is(true));
      assertThat(service.getPools(), hasSize(1));
      return true;
    }));
  }

  private SchedulerPoolsConfigFactory getMockConfigFactory() {
    final SchedulerPoolsConfigFactory configFactory = mock(SchedulerPoolsConfigFactory.class);
    final SchedulerPoolsConfig config = getMockConfig();
    when(configFactory.getConfig()).thenReturn(of(config));
    return configFactory;
  }

  private SchedulerPoolsConfig getMockConfig() {
    final SchedulerPoolsConfig config = mock(SchedulerPoolsConfig.class);
    when(config.getGracefulShutdownTimeout()).thenReturn(OptionalLong.of(30000L));
    when(config.getCpuLightPoolSize()).thenReturn(OptionalInt.of(1));
    when(config.getCpuLightQueueSize()).thenReturn(OptionalInt.of(1));
    when(config.getCpuIntensiveQueueSize()).thenReturn(OptionalInt.of(1));
    when(config.getIoCorePoolSize()).thenReturn(OptionalInt.of(1));
    when(config.getIoMaxPoolSize()).thenReturn(OptionalInt.of(1));
    when(config.getIoKeepAlive()).thenReturn(OptionalLong.of(30000L));
    when(config.getCpuIntensivePoolSize()).thenReturn(OptionalInt.of(1));
    when(config.getCpuIntensiveQueueSize()).thenReturn(OptionalInt.of(1));
    when(config.getThreadNamePrefix()).thenReturn("test");
    return config;
  }
}
