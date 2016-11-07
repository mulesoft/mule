/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.notification;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.context.notification.BlockingServerEvent;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.service.scheduler.internal.DefaultSchedulerService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.Required;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;
import org.junit.Test;

public class ServerNotificationManagerPerformanceTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

  private static final int K_NOTIFICATIONS = 1000;

  private DefaultSchedulerService schedulerService;
  private ServerNotificationManager notificationManager;

  private final PerfTestServerNotificationListener asyncListener = new PerfTestServerNotificationListener();
  private final PerfTestServerBlockingNotificationListener syncListener = new PerfTestServerBlockingNotificationListener();

  public ServerNotificationManagerPerformanceTestCase() {
    setStartContext(true);
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();

    schedulerService = new DefaultSchedulerService();
    schedulerService.start();
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    notificationManager = muleContext.getNotificationManager();
    notificationManager.addInterfaceToType(PerfTestServerNotificationListener.class, PerfTestServerNotification.class);
    notificationManager.addInterfaceToType(PerfTestServerBlockingNotificationListener.class,
                                           PerfTestServerBlockingNotification.class);
    notificationManager.addListener(syncListener);
    notificationManager.addListener(asyncListener);
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        MuleRegistry registry = muleContext.getRegistry();
        registry.registerObject(schedulerService.getName(), schedulerService);
      }
    });
  }

  @Override
  protected void doTearDown() throws Exception {
    notificationManager.removeListener(asyncListener);
    notificationManager.removeListener(syncListener);
    super.doTearDown();
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    schedulerService.stop();
    super.doTearDownAfterMuleContextDispose();
  }

  @Test
  @PerfTest(duration = 30000, threads = 1, warmUp = 10000)
  @Required(throughput = 600)
  public void dispatchBlockingEvents() {
    syncListener.reset();
    for (int i = 0; i < K_NOTIFICATIONS; ++i) {
      notificationManager.fireNotification(new PerfTestServerBlockingNotification());
    }

    assertThat(syncListener.getNotifications(), is(K_NOTIFICATIONS));
  }

  @Test
  @PerfTest(duration = 30000, threads = 1, warmUp = 10000)
  @Required(throughput = 350)
  public void justDispatchAsyncEvents() {
    asyncListener.reset();
    for (int i = 0; i < K_NOTIFICATIONS; ++i) {
      notificationManager.fireNotification(new PerfTestServerNotification());
    }
  }

  @Test
  @PerfTest(duration = 30000, threads = 1, warmUp = 10000)
  @Required(throughput = 60)
  public void dispatchAndNotifyAsyncEvents() {
    asyncListener.reset();
    for (int i = 0; i < K_NOTIFICATIONS; ++i) {
      notificationManager.fireNotification(new PerfTestServerNotification());
    }

    new PollingProber(1000, 10).check(new JUnitLambdaProbe(() -> asyncListener.getNotifications() >= K_NOTIFICATIONS));
  }

  public static class PerfTestServerNotificationListener implements ServerNotificationListener<PerfTestServerNotification> {

    private final AtomicInteger notifications = new AtomicInteger();

    @Override
    public void onNotification(PerfTestServerNotification notification) {
      notifications.incrementAndGet();
    }

    public int getNotifications() {
      return notifications.get();
    }

    public void reset() {
      notifications.set(0);
    }
  }

  public static class PerfTestServerBlockingNotificationListener
      implements ServerNotificationListener<PerfTestServerBlockingNotification> {

    private final AtomicInteger notifications = new AtomicInteger();

    @Override
    public void onNotification(PerfTestServerBlockingNotification notification) {
      notifications.incrementAndGet();
    }

    public int getNotifications() {
      return notifications.get();
    }

    public void reset() {
      notifications.set(0);
    }
  }

  public static class PerfTestServerNotification extends ServerNotification {

    static {
      registerAction("PerfTestServerNotification", CUSTOM_EVENT_ACTION_START_RANGE + 1);
    }

    public PerfTestServerNotification() {
      super("", CUSTOM_EVENT_ACTION_START_RANGE + 1);
    }

  }

  public static class PerfTestServerBlockingNotification extends ServerNotification implements BlockingServerEvent {

    static {
      registerAction("PerfTestServerBlockingNotification", CUSTOM_EVENT_ACTION_START_RANGE + 2);
    }

    public PerfTestServerBlockingNotification() {
      super("", CUSTOM_EVENT_ACTION_START_RANGE + 2);
    }

  }

}
