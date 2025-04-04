/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.lifecycle.MuleLifecycleInterceptor;
import org.mule.runtime.core.internal.registry.map.RegistryMap;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.feature.internal.config.DefaultFeatureFlaggingService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.slf4j.Logger;

import org.junit.Test;

import io.qameta.allure.Issue;

public class SimpleRegistryTestCase extends AbstractMuleContextTestCase {

  private static final String LIFECYCLE_PHASES = "[setMuleContext, initialise, start, stop, dispose]";
  public static final String TEST_KEY = "test";

  @Test
  public void testObjectLifecycle() throws Exception {
    muleContext.start();

    InterfaceBasedTracker tracker = new InterfaceBasedTracker();
    getRegistry().registerObject(TEST_KEY, tracker);

    muleContext.dispose();
    assertEquals(LIFECYCLE_PHASES, tracker.getTracker().toString());
  }

  @Test
  public void testObjectLifecycleJavax() throws Exception {
    muleContext.start();

    InterfaceBasedTrackerJavax tracker = new InterfaceBasedTrackerJavax();
    getRegistry().registerObject(TEST_KEY, tracker);

    muleContext.dispose();
    assertEquals(LIFECYCLE_PHASES, tracker.getTracker().toString());
  }

  @Test
  public void featureFlaggingService() throws Exception {
    muleContext.start();

    DefaultFeatureFlaggingService featureFlaggingService =
        (DefaultFeatureFlaggingService) getRegistry().get(FEATURE_FLAGGING_SERVICE_KEY);

    assertThat(featureFlaggingService.getArtfactName(), equalTo("SimpleRegistryTestCase#featureFlaggingService"));
  }

  @Test
  public void testObjectLifecycleDoubleRegistration() throws Exception {
    muleContext.start();

    InterfaceBasedTracker tracker1 = new InterfaceBasedTracker();
    getRegistry().registerObject(TEST_KEY, tracker1);

    InterfaceBasedTracker tracker2 = new InterfaceBasedTracker();
    getRegistry().registerObject(TEST_KEY, tracker2);

    InterfaceBasedTracker tracker3 = new InterfaceBasedTracker();
    getRegistry().registerObject(TEST_KEY, tracker3);

    muleContext.dispose();
    assertEquals(LIFECYCLE_PHASES, tracker1.getTracker().toString());
    assertEquals(LIFECYCLE_PHASES, tracker2.getTracker().toString());
    assertEquals(LIFECYCLE_PHASES, tracker3.getTracker().toString());
  }

  @Test
  public void doesNotTracksNonDisposableOverriddenObjects() throws Exception {
    final Logger log = mock(Logger.class);
    final RegistryMap registryMap = new RegistryMap(log);
    Object value1 = new Object();
    Object value2 = new Object();
    registryMap.putAndLogWarningIfDuplicate(TEST_KEY, value1);
    registryMap.putAndLogWarningIfDuplicate(TEST_KEY, value2);

    assertThat(registryMap.getLostObjects(), is(empty()));
  }

  @Test
  public void testObjectLifecycleStates() throws Exception {
    InterfaceBasedTracker tracker = new InterfaceBasedTracker();
    getRegistry().registerObject(TEST_KEY, tracker);
    assertEquals("[setMuleContext, initialise]", tracker.getTracker().toString());

    try {
      muleContext.initialise();
      fail("context already initialised");
    } catch (IllegalStateException e) {
      // expected
    }

    muleContext.start();
    assertEquals("[setMuleContext, initialise, start]", tracker.getTracker().toString());

    try {
      muleContext.start();
      fail("context already started");
    } catch (IllegalStateException e) {
      // expected
    }

    muleContext.stop();
    assertEquals("[setMuleContext, initialise, start, stop]", tracker.getTracker().toString());

    try {
      muleContext.stop();
      fail("context already stopped");
    } catch (IllegalStateException e) {
      // expected
    }

    muleContext.dispose();
    assertEquals(LIFECYCLE_PHASES, tracker.getTracker().toString());

    try {
      muleContext.dispose();
      fail("context already disposed");
    } catch (IllegalStateException e) {
      // expected
    }
  }

  @Test
  public void testObjectLifecycleRestart() throws Exception {
    InterfaceBasedTracker tracker = new InterfaceBasedTracker();
    getRegistry().registerObject(TEST_KEY, tracker);

    muleContext.start();
    assertEquals("[setMuleContext, initialise, start]", tracker.getTracker().toString());

    muleContext.stop();
    assertEquals("[setMuleContext, initialise, start, stop]", tracker.getTracker().toString());

    muleContext.start();
    assertEquals("[setMuleContext, initialise, start, stop, start]", tracker.getTracker().toString());

    muleContext.dispose();
    assertEquals("[setMuleContext, initialise, start, stop, start, stop, dispose]", tracker.getTracker().toString());
  }

  @Test
  public void testLifecycleStateOutOfSequenceDisposeFirstWithTransientRegistryDirectly() throws Exception {
    SimpleRegistry reg = new SimpleRegistry(muleContext, new MuleLifecycleInterceptor());

    reg.fireLifecycle(Disposable.PHASE_NAME);

    InterfaceBasedTracker tracker = new InterfaceBasedTracker();
    try {
      reg.registerObject(TEST_KEY, tracker);
      fail("Cannot register objects on a disposed registry");
    } catch (RegistrationException e) {
      // Expected
    }
  }

  @Test
  public void testLifecycleStateOutOfSequenceStartFirst() throws Exception {
    muleContext.start();
    InterfaceBasedTracker tracker = new InterfaceBasedTracker();
    getRegistry().registerObject(TEST_KEY, tracker);
    // Initialise called implicitly because you cannot start a component without initialising it first
    assertEquals("[setMuleContext, initialise, start]", tracker.getTracker().toString());

    muleContext.dispose();
    // Stop called implicitly because you cannot dispose component without stopping it first
    assertEquals(LIFECYCLE_PHASES, tracker.getTracker().toString());
  }

  @Test
  public void testLifecycleStateOutOfSequenceStopFirst() throws Exception {
    try {
      muleContext.stop();
      fail("Cannot not stop the context if not started");
    } catch (IllegalStateException e) {
      // expected
    }

    muleContext.start();
    muleContext.stop();
    InterfaceBasedTracker tracker = new InterfaceBasedTracker();
    getRegistry().registerObject(TEST_KEY, tracker);
    // Start is bypassed because the component was added when the registry was stopped, hence no need to start the component
    // Stop isn't called either because start was not called
    // Initialised is called because that pahse has completed in the registry
    assertEquals("[setMuleContext, initialise]", tracker.getTracker().toString());

    muleContext.dispose();
    assertEquals("[setMuleContext, initialise, dispose]", tracker.getTracker().toString());
  }

  @Test
  public void testLifecycleStateOutOfSequenceDisposeFirst() throws Exception {
    muleContext.dispose();

    InterfaceBasedTracker tracker = new InterfaceBasedTracker();
    try {
      getRegistry().registerObject(TEST_KEY, tracker);
      fail("cannot register objects on a disposed registry");
    } catch (RegistrationException e) {
      // Expected
    }
  }

  @Test
  @Issue("W-14547712")
  public void lifecycleCallbackOnRegistryWithoutMuleContext() throws Exception {
    final SimpleRegistry registry = new SimpleRegistry(null);

    final InterfaceBasedTracker lifecycleTracker = new InterfaceBasedTracker();
    registry.registerObject("_testLifecycle", lifecycleTracker);

    assertThat(registry.getLifecycleManager().getCurrentPhase(), is("not in lifecycle"));
    assertThat(lifecycleTracker.getTracker(), emptyIterable());

    registry.initialise();
    assertThat(registry.getLifecycleManager().getCurrentPhase(), is("initialise"));
    assertThat(lifecycleTracker.getTracker(), hasItems("initialise"));

    registry.dispose();
    assertThat(registry.getLifecycleManager().getCurrentPhase(), is("dispose"));
    assertThat(lifecycleTracker.getTracker(), hasItems("initialise", "dispose"));
  }

  private MuleRegistry getRegistry() {
    return ((MuleContextWithRegistry) muleContext).getRegistry();
  }

  public class InterfaceBasedTracker extends AbstractLifecycleTracker {
    // no custom methods
  }

  public class InterfaceBasedTrackerJavax extends AbstractLifecycleTrackerJavax {
    // no custom methods
  }

}
