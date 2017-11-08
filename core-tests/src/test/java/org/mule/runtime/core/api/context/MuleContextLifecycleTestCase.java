/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STARTED;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STARTING;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STOPPED;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STOPPING;
import static org.mule.tck.MuleAssert.assertTrue;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.internal.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.internal.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationListenerRegistry;
import org.mule.runtime.core.internal.lifecycle.MuleContextLifecycleManager;
import org.mule.runtime.core.internal.util.JdkVersionUtils;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MuleContextLifecycleTestCase extends AbstractMuleTestCase {

  private MuleContextBuilder ctxBuilder;
  private SensingLifecycleManager lifecycleManager;
  private MuleContext ctx;
  private TestMuleContextListener callbackListener;
  private NotificationListenerRegistry notificationListenerRegistry;

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  @Before
  public void setup() throws Exception {
    ctxBuilder = new DefaultMuleContextBuilder(APP);
    lifecycleManager = new SensingLifecycleManager();
    ctxBuilder.setLifecycleManager(lifecycleManager);
    callbackListener = new TestMuleContextListener();
    ctxBuilder.setListeners(Arrays.asList(callbackListener));
    ctx = ctxBuilder.buildMuleContext();

    notificationListenerRegistry = new DefaultNotificationListenerRegistry();
    ((MuleContextWithRegistries) ctx).getRegistry().registerObject(NotificationListenerRegistry.REGISTRY_KEY,
                                                                   notificationListenerRegistry);
    testServicesConfigurationBuilder.configure(ctx);
  }

  @After
  public void tearDown() throws Exception {
    if (ctx != null && !ctx.isDisposed()) {
      ctx.dispose();
    }
  }

  //
  // Initialize
  //
  @Test
  public void initaliseSuccessful() throws Exception {
    assertFalse(ctx.isInitialised());
    assertFalse(ctx.isInitialising());
    assertFalse(ctx.isStarted());
    assertFalse(ctx.isDisposed());
    assertFalse(ctx.isDisposing());

    ctx.initialise();
    assertTrue(ctx.isInitialised());
    assertFalse(ctx.isInitialising());
    assertFalse(ctx.isStarted());
    assertFalse(ctx.isDisposed());
    assertFalse(ctx.isDisposing());
  }

  @Test(expected = IllegalStateException.class)
  public void initialiseOnInitialised() throws MuleException {
    ctx.initialise();

    // Can't call twice
    ctx.initialise();
  }

  @Test(expected = IllegalStateException.class)
  public void initialiseOnStarted() throws Exception {
    ctx.initialise();
    new DefaultsConfigurationBuilder().configure(ctx);
    ctx.start();

    // Attempt to initialise once started should fail!
    ctx.initialise();
  }

  @Test(expected = IllegalStateException.class)
  public void initialiseOnStopped() throws Exception {
    ctx.initialise();
    new DefaultsConfigurationBuilder().configure(ctx);
    ctx.start();
    ctx.stop();

    // Attempt to initialise once stopped should fail!
    ctx.initialise();
  }

  @Test(expected = IllegalStateException.class)
  public void initialiseOnDisposed() throws Exception {
    ctx.initialise();
    new DefaultsConfigurationBuilder().configure(ctx);
    ctx.start();
    ctx.stop();
    ctx.dispose();

    // Attempt to initialise once disposed should fail!
    ctx.initialise();
  }

  //
  // Start
  //
  @Test(expected = IllegalStateException.class)
  public void startBeforeInitialise() throws Exception {
    ctx.start();
  }

  @Test
  public void startOnInitialised() throws Exception {
    ctx.initialise();

    new DefaultsConfigurationBuilder().configure(ctx);
    TestNotificationListener notificationListener = new TestNotificationListener();
    notificationListenerRegistry.registerListener(notificationListener);
    ctx.start();

    assertTrue(ctx.isInitialised());
    assertFalse(ctx.isInitialising());
    assertTrue(ctx.isStarted());
    assertFalse(ctx.isDisposed());
    assertFalse(ctx.isDisposing());

    assertTrue("CONTEXT_STARTING notification never fired", notificationListener.startingNotificationFired);
    assertTrue("CONTEXT_STARTED notification never fired", notificationListener.startedNotificationFired);

    assertTrue("onInitialization never called on listener", callbackListener.wasInitialized);
    assertTrue("onStart never called on listener", callbackListener.wasStarted);
  }

  @Test(expected = IllegalStateException.class)
  public void startOnStarted() throws Exception {
    ctx.initialise();
    assertTrue("onInitialization never called on listener", callbackListener.wasInitialized);

    new DefaultsConfigurationBuilder().configure(ctx);
    TestNotificationListener notificationListener = new TestNotificationListener();
    notificationListenerRegistry.registerListener(notificationListener);
    ctx.start();

    assertTrue("CONTEXT_STARTING notification never fired", notificationListener.startingNotificationFired);
    assertTrue("CONTEXT_STARTED notification never fired", notificationListener.startedNotificationFired);
    assertTrue("onStart never called on listener", callbackListener.wasStarted);

    // Can't call twice
    ctx.start();
  }

  @Test
  public void startOnStopped() throws Exception {
    ctx.initialise();
    new DefaultsConfigurationBuilder().configure(ctx);
    ctx.start();

    ctx.stop();
    ctx.start();
    assertTrue(ctx.isInitialised());
    assertFalse(ctx.isInitialising());
    assertTrue(ctx.isStarted());
    assertFalse(ctx.isDisposed());
    assertFalse(ctx.isDisposing());
  }

  @Test(expected = IllegalStateException.class)
  public void startOnDisposed() throws Exception {
    ctx.initialise();
    ctx.dispose();

    // Attempt to start once disposed should fail!
    ctx.start();
  }

  //
  // Stop
  //
  @Test(expected = IllegalStateException.class)
  public void stopBeforeInitialise() throws Exception {
    // Attempt to stop before initialise should fail!
    ctx.stop();
  }

  @Test(expected = IllegalStateException.class)
  public void stopOnInitialised() throws Exception {
    ctx.initialise();

    // cannot stop if not started
    ctx.stop();
  }

  @Test
  public void stopOnStarted() throws Exception {
    buildStartedMuleContext();

    ctx.stop();
    assertTrue(ctx.isInitialised());
    assertFalse(ctx.isInitialising());
    assertFalse(ctx.isStarted());
    assertFalse(ctx.isDisposed());
    assertFalse(ctx.isDisposing());
  }

  @Test(expected = IllegalStateException.class)
  public void stopOnStopped() throws Exception {
    buildStartedMuleContext();
    ctx.stop();

    // Can't call twice
    ctx.stop();
  }

  @Test(expected = IllegalStateException.class)
  public void stopOnDisposed() throws Exception {
    buildStartedMuleContext();
    ctx.stop();
    ctx.dispose();

    // Attempt to stop once disposed should fail!
    ctx.stop();
  }

  //
  // Dispose
  //
  @Test
  public void disposeBeforeInitialised() {
    ctx.dispose();
    assertFalse(ctx.isInitialised());
    assertFalse(ctx.isInitialising());
    assertFalse(ctx.isStarted());
    assertTrue(ctx.isDisposed());
    assertFalse(ctx.isDisposing());

    assertLifecycleManagerDidApplyPhases(Disposable.PHASE_NAME);
  }

  @Test
  public void disposeOnInitialised() throws Exception {
    ctx.initialise();
    ctx.dispose();
    assertFalse(ctx.isInitialised());
    assertFalse(ctx.isInitialising());
    assertFalse(ctx.isStarted());
    assertTrue(ctx.isDisposed());
    assertFalse(ctx.isDisposing());

    assertLifecycleManagerDidApplyPhases(Initialisable.PHASE_NAME, Disposable.PHASE_NAME);
  }

  @Test
  public void disposeOnStarted() throws Exception {
    ctx.initialise();
    assertTrue("onInitialization never called on listener", callbackListener.wasInitialized);

    new DefaultsConfigurationBuilder().configure(ctx);
    final TestNotificationListener notificationListener = new TestNotificationListener();
    notificationListenerRegistry.registerListener(notificationListener);

    ctx.start();
    assertTrue("onStart never called on listener", callbackListener.wasStarted);
    ctx.dispose();
    assertFalse(ctx.isInitialised());
    assertFalse(ctx.isInitialising());
    assertFalse(ctx.isStarted());
    assertTrue(ctx.isDisposed());
    assertFalse(ctx.isDisposing());

    // disposing started must go through stop
    assertLifecycleManagerDidApplyAllPhases();

    assertTrue("CONTEXT_STOPPING notification never fired", notificationListener.stoppingNotificationFired.get());
    assertTrue("CONTEXT_STOPPED notification never fired", notificationListener.stoppedNotificationFired.get());
    assertTrue("onStop never called on listener", callbackListener.wasStopped);
  }

  @Test
  public void disposeOnStopped() throws Exception {
    ctx.initialise();
    new DefaultsConfigurationBuilder().configure(ctx);
    ctx.start();
    ctx.stop();
    ctx.dispose();
    assertFalse(ctx.isInitialised());
    assertFalse(ctx.isInitialising());
    assertFalse(ctx.isStarted());
    assertTrue(ctx.isDisposed());
    assertFalse(ctx.isDisposing());

    assertLifecycleManagerDidApplyAllPhases();
  }

  @Test(expected = IllegalStateException.class)
  public void disposeOnDisposed() throws Exception {
    ctx.initialise();
    ctx.dispose();

    // can't call twice
    ctx.dispose();
  }

  @Test
  public void notificationHasMuleContextRef() throws Exception {
    ctx.initialise();
    new DefaultsConfigurationBuilder().configure(ctx);

    final AtomicReference<MuleContext> contextFromNotification = new AtomicReference<>();
    final AtomicReference<String> resourceId = new AtomicReference<>();
    MuleContextNotificationListener<MuleContextNotification> listener =
        notification -> {
          contextFromNotification.set(notification.getMuleContext());
          resourceId.set(notification.getResourceIdentifier());
        };
    notificationListenerRegistry.registerListener(listener);
    ctx.start();

    assertNotNull(contextFromNotification.get());
    assertSame(ctx, contextFromNotification.get());
    assertEquals(ctx.getConfiguration().getId(), resourceId.get());
  }

  private MuleContext buildStartedMuleContext() throws Exception {
    ctx.initialise();

    // DefaultMuleContext refuses to start without these objects in place
    SecurityManager securityManager = mock(SecurityManager.class);
    ((MuleContextWithRegistries) ctx).getRegistry().registerObject(UUID.getUUID(), securityManager);

    QueueManager queueManager = mock(QueueManager.class);
    ((MuleContextWithRegistries) ctx).getRegistry().registerObject(UUID.getUUID(), queueManager);

    ctx.start();
    return ctx;
  }

  private void assertLifecycleManagerDidApplyPhases(String... phaseNames) {
    assertTrue(lifecycleManager.didApplyPhases(phaseNames));
  }

  private void assertLifecycleManagerDidApplyAllPhases() {
    assertLifecycleManagerDidApplyPhases(Initialisable.PHASE_NAME, Startable.PHASE_NAME, Stoppable.PHASE_NAME,
                                         Disposable.PHASE_NAME);
  }

  private static class SensingLifecycleManager extends MuleContextLifecycleManager {

    private List<String> appliedLifecyclePhases;

    public SensingLifecycleManager() {
      super();
      appliedLifecyclePhases = new ArrayList<>();
    }

    public boolean didApplyPhases(String... phaseNames) {
      List<String> expectedPhases = Arrays.asList(phaseNames);
      return expectedPhases.equals(appliedLifecyclePhases);
    }

    @Override
    public void fireLifecycle(String phase) throws LifecycleException {
      appliedLifecyclePhases.add(phase);
      super.fireLifecycle(phase);
    }
  }

  static class TestNotificationListener implements MuleContextNotificationListener<MuleContextNotification> {

    final AtomicBoolean startingNotificationFired = new AtomicBoolean(false);
    final AtomicBoolean startedNotificationFired = new AtomicBoolean(false);
    final AtomicBoolean stoppingNotificationFired = new AtomicBoolean(false);
    final AtomicBoolean stoppedNotificationFired = new AtomicBoolean(false);

    @Override
    public boolean isBlocking() {
      return false;
    }

    @Override
    public void onNotification(MuleContextNotification notification) {
      switch (notification.getAction().getActionId()) {
        case CONTEXT_STARTING:
          startingNotificationFired.set(true);
          break;

        case CONTEXT_STARTED:
          startedNotificationFired.set(true);
          break;

        case CONTEXT_STOPPING:
          stoppingNotificationFired.set(true);
          break;

        case CONTEXT_STOPPED:
          stoppedNotificationFired.set(true);
          break;
      }
    }
  }

  static class TestMuleContextListener implements MuleContextListener {

    final AtomicBoolean wasCreated = new AtomicBoolean(false);
    final AtomicBoolean wasInitialized = new AtomicBoolean(false);
    final AtomicBoolean wasStarted = new AtomicBoolean(false);
    final AtomicBoolean wasStopped = new AtomicBoolean(false);

    @Override
    public void onCreation(MuleContext context) {
      wasCreated.set(true);
    }

    @Override
    public void onInitialization(MuleContext context, Registry registry) {
      wasInitialized.set(true);
    }

    @Override
    public void onStart(MuleContext context, Registry registry) {
      wasStarted.set(true);
    }

    @Override
    public void onStop(MuleContext context, Registry registry) {
      wasStopped.set(true);
    }
  }

  @Test(expected = InitialisationException.class)
  public void testIsInValidJdk() throws InitialisationException {
    try {
      JdkVersionUtils.validateJdk();
    } catch (RuntimeException e) {
      fail("Jdk version or vendor is invalid. Update the valid versions");
    }

    String javaVersion = System.setProperty("java.version", "1.5.0_12");
    try {
      try {
        JdkVersionUtils.validateJdk();
        fail("Test is invalid because the Jdk version or vendor is supposed to now be invalid");
      } catch (RuntimeException e) {
        // expected
      }

      MuleContext ctx = ctxBuilder.buildMuleContext();
      assertFalse(ctx.isInitialised());
      assertFalse(ctx.isInitialising());
      assertFalse(ctx.isStarted());
      assertFalse(ctx.isDisposed());
      assertFalse(ctx.isDisposing());

      ctx.initialise();
    } finally {
      System.setProperty("java.version", javaVersion);
    }
  }
}
