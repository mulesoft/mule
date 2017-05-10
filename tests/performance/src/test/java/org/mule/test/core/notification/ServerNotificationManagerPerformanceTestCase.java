/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.notification;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.context.notification.SynchronousServerEvent;
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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-11450: Migrate Contiperf tests to JMH")
public class ServerNotificationManagerPerformanceTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

  private static final int K_NOTIFICATIONS = 100;

  private DefaultSchedulerService schedulerService;
  private ServerNotificationManager notificationManager;

  private final PerfTestIOServerNotificationListener asyncIOListener = new PerfTestIOServerNotificationListener();
  private final PerfTestLightServerNotificationListener asyncLightListener = new PerfTestLightServerNotificationListener();
  private final PerfTestServerSynchronousNotificationListener syncListener = new PerfTestServerSynchronousNotificationListener();

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
    notificationManager.addInterfaceToType(PerfTestIOServerNotificationListener.class, PerfTestIOServerNotification.class);
    notificationManager.addInterfaceToType(PerfTestLightServerNotificationListener.class, PerfTestLightServerNotification.class);
    notificationManager.addInterfaceToType(PerfTestServerSynchronousNotificationListener.class,
                                           PerfTestServerSynchronousNotification.class);
    notificationManager.addListener(syncListener);
    notificationManager.addListener(asyncIOListener);
    notificationManager.addListener(asyncLightListener);
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
    notificationManager.removeListener(asyncIOListener);
    notificationManager.removeListener(asyncLightListener);
    notificationManager.removeListener(syncListener);
    super.doTearDown();
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    schedulerService.stop();
    super.doTearDownAfterMuleContextDispose();
  }

  @Test
  @PerfTest(duration = 50000, threads = 1, warmUp = 10000)
  @Required(throughput = 30000)
  public void dispatchSynchronousEvents() {
    syncListener.reset();
    for (int i = 0; i < K_NOTIFICATIONS; ++i) {
      notificationManager.fireNotification(new PerfTestServerSynchronousNotification());
    }

    assertThat(syncListener.getNotifications(), is(K_NOTIFICATIONS));
  }

  @Test
  @PerfTest(duration = 50000, threads = 1, warmUp = 10000)
  @Required(throughput = 600)
  public void justDispatchAsyncIOEvents() {
    asyncIOListener.reset();
    for (int i = 0; i < K_NOTIFICATIONS; ++i) {
      notificationManager.fireNotification(new PerfTestIOServerNotification());
    }
  }

  @Test
  @PerfTest(duration = 50000, threads = 1, warmUp = 10000)
  @Required(throughput = 10000)
  public void justDispatchAsyncLightEvents() {
    asyncLightListener.reset();
    for (int i = 0; i < K_NOTIFICATIONS; ++i) {
      notificationManager.fireNotification(new PerfTestLightServerNotification());
    }
  }

  @Test
  @PerfTest(duration = 50000, threads = 1, warmUp = 10000)
  @Required(throughput = 6)
  public void dispatchAndNotifyIOAsyncEvents() {
    asyncIOListener.reset();
    for (int i = 0; i < K_NOTIFICATIONS; ++i) {
      notificationManager.fireNotification(new PerfTestIOServerNotification());
    }

    new PollingProber(5000, 10).check(new JUnitLambdaProbe(() -> asyncIOListener.getNotifications() >= K_NOTIFICATIONS));
  }

  @Test
  @PerfTest(duration = 50000, threads = 1, warmUp = 10000)
  @Required(throughput = 80)
  public void dispatchAndNotifyAsyncLightEvents() {
    asyncLightListener.reset();
    for (int i = 0; i < K_NOTIFICATIONS; ++i) {
      notificationManager.fireNotification(new PerfTestLightServerNotification());
    }

    new PollingProber(5000, 10).check(new JUnitLambdaProbe(() -> asyncLightListener.getNotifications() >= K_NOTIFICATIONS));
  }

  public static class PerfTestLightServerNotificationListener
      implements ServerNotificationListener<PerfTestLightServerNotification> {

    private final AtomicInteger notifications = new AtomicInteger();

    @Override
    public void onNotification(PerfTestLightServerNotification notification) {
      notifications.incrementAndGet();
    }

    public int getNotifications() {
      return notifications.get();
    }

    public void reset() {
      notifications.set(0);
    }

    @Override
    public boolean isBlocking() {
      return false;
    }
  }

  public static class PerfTestIOServerNotificationListener implements ServerNotificationListener<PerfTestIOServerNotification> {

    private final AtomicInteger notifications = new AtomicInteger();

    @Override
    public void onNotification(PerfTestIOServerNotification notification) {
      try {
        sleep(10);
      } catch (InterruptedException e) {
        currentThread().interrupt();
      }
      notifications.incrementAndGet();
    }

    public int getNotifications() {
      return notifications.get();
    }

    public void reset() {
      notifications.set(0);
    }
  }

  public static class PerfTestServerSynchronousNotificationListener
      implements ServerNotificationListener<PerfTestServerSynchronousNotification> {

    private final AtomicInteger notifications = new AtomicInteger();

    @Override
    public boolean isBlocking() {
      return false;
    }

    @Override
    public void onNotification(PerfTestServerSynchronousNotification notification) {
      notifications.incrementAndGet();
    }

    public int getNotifications() {
      return notifications.get();
    }

    public void reset() {
      notifications.set(0);
    }
  }

  public static class PerfTestIOServerNotification extends ServerNotification {

    static {
      registerAction("PerfTestIOServerNotification", CUSTOM_EVENT_ACTION_START_RANGE + 1);
    }

    public PerfTestIOServerNotification() {
      super("", CUSTOM_EVENT_ACTION_START_RANGE + 1);
    }

  }

  public static class PerfTestLightServerNotification extends ServerNotification {

    static {
      registerAction("PerfTestLightServerNotification", CUSTOM_EVENT_ACTION_START_RANGE + 2);
    }

    public PerfTestLightServerNotification() {
      super("", CUSTOM_EVENT_ACTION_START_RANGE + 2);
    }

  }

  public static class PerfTestServerSynchronousNotification extends ServerNotification implements SynchronousServerEvent {

    static {
      registerAction("PerfTestServerBlockingNotification", CUSTOM_EVENT_ACTION_START_RANGE + 3);
    }

    public PerfTestServerSynchronousNotification() {
      super("", CUSTOM_EVENT_ACTION_START_RANGE + 3);
    }

  }

}
