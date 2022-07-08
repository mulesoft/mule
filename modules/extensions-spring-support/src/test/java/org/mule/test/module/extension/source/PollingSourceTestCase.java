/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.lang.String.valueOf;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.api.notification.PollingSourceItemNotification.ITEM_DISPATCHED;
import static org.mule.runtime.module.extension.internal.runtime.source.poll.PollItemNotificationAction.ACCEPTED_ITEM;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.tck.probe.PollingProber.checkNot;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.POLLING;
import static org.mule.test.petstore.extension.NumberPetAdoptionSource.ALL_NUMBERS;
import static org.mule.test.petstore.extension.PetAdoptionSource.ALL_PETS;
import static org.mule.test.petstore.extension.PetAdoptionSource.FAILED_ADOPTION_COUNT;
import static org.mule.test.petstore.extension.PetAdoptionSource.STARTED_POLLS;
import static org.mule.test.petstore.extension.PetFailingPollingSource.POLL_INVOCATIONS;
import static org.mule.test.petstore.extension.PetFailingPollingSource.STARTED_SOURCES;
import static org.mule.test.petstore.extension.WatermarkingPetAdoptionSource.resetSource;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.notification.ExtensionNotification;
import org.mule.runtime.api.notification.ExtensionNotificationListener;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.api.notification.PipelineMessageNotificationListener;
import org.mule.runtime.api.notification.PollingSourceItemNotification;
import org.mule.runtime.api.notification.PollingSourceItemNotificationListener;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetAdoptionSource;
import org.mule.test.petstore.extension.PetFailingPollingSource;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SOURCES)
@Story(POLLING)
public class PollingSourceTestCase extends AbstractExtensionFunctionalTestCase {

  private static final int TIMEOUT = 5000;
  private static final int DELAY = 100;
  private static final List<CoreEvent> ADOPTION_EVENTS = new LinkedList<>();

  public static class AdoptionProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (ADOPTION_EVENTS) {
        ADOPTION_EVENTS.add(event);
      }
      return event;
    }
  }

  @Override
  protected void doTearDown() throws Exception {
    ADOPTION_EVENTS.clear();
    resetSource();
  }

  @Override
  protected String getConfigFile() {
    return "source/polling-source-config.xml";
  }

  @Before
  public void resetCounters() throws Exception {
    PetFailingPollingSource.STARTED_POLLS = 0;
    POLL_INVOCATIONS.clear();
    STARTED_SOURCES.clear();
    STARTED_POLLS = 0;
  }

  @Test
  public void vanillaPoll() throws Exception {
    startFlow("vanilla");
    assertAllPetsAdopted();

    check(TIMEOUT, DELAY * 2, () -> {
      synchronized (ADOPTION_EVENTS) {
        return PetAdoptionSource.COMPLETED_POLLS > 1 &&
            PetAdoptionSource.ADOPTED_PET_COUNT >= ADOPTION_EVENTS.size();
      }
    });
  }

  @Test
  public void idempotentPoll() throws Exception {
    startFlow("idempotent");
    check(TIMEOUT, DELAY, () -> {
      synchronized (ADOPTION_EVENTS) {
        return PetAdoptionSource.REJECTED_ADOPTIONS >= ALL_PETS.size() &&
            ALL_PETS.containsAll(ADOPTION_EVENTS.stream()
                .map(e -> e.getMessage().getPayload().getValue().toString())
                .collect(toList()));
      }
    });
    assertIdempotentAdoptions();
  }

  @Test
  public void idempotentLocksAreReleased() throws Exception {
    startFlow("idempotentLocksAreReleased");
    assertAllPetsAdopted();
    doTearDown();
    assertAllPetsAdopted();
  }

  @Test
  public void watermarkPoll() throws Exception {
    startFlow("watermark");
    assertAllPetsAdopted();

    assertIdempotentAdoptions();
  }

  @Test
  public void failingPoll() throws Exception {
    startFlow("failingPoll");
    check(TIMEOUT, DELAY, () -> FAILED_ADOPTION_COUNT >= ALL_PETS.size());
  }

  @Test
  public void multiplePhasesOfWatermarkPoll() throws Exception {
    startFlow("multiplePhasesOfWaterMark");
    assertIdempotentAdoptions();
  }

  @Test
  public void multiplePhasesOfWatermarkWithIncreasingAndDecreasingWatermarksPoll() throws Exception {
    startFlow("multiplePhasesOfWatermarkWithIncreasingAndDecreasingWatermarks");
    assertAllNumbersAdoptedExactlyOnce();
  }

  /*
   * This test checks that when a polling source with a fixed frequency scheduler with start delay is restarted, the start delay
   * is not applied again. The polling source of this test is set to fail midway through populating the pet adoption list, which
   * will provoke a restart. Without the changes made in MULE-16974, this test would fail, because the start delay would be
   * re-applied on the restart and the probe would timeout.
   */
  @Test
  public void whenReconnectingAfterConnectionExceptionSchedulerRunsWithoutStartDelay() throws Exception {
    startFlow("fixedFrequencyReconnectingPoll");
    assertAllPetsAdopted();
  }

  @Description("This test reflects a behavior that we must preserve, when a polling source is stopped and started the scheduler must be stopped and a new one must be started.")
  @Test
  public void whenSourceIsStopAndStartedSchedulerIsReset() throws Exception {
    startFlow("longFrequencyPoll");
    assertStartedPolls(1);
    stopFlow("longFrequencyPoll");
    sleep(1000);
    startFlow("longFrequencyPoll");
    assertStartedPolls(1);
  }

  @Test
  public void sourceRetriggersImmediatlyOnReconnection() throws Exception {
    startFlow("failingLongFrequencyPoll");
    assertPetFailingSourcePollsFromDifferentSources(2);
  }

  @Test
  public void sourcePollFailWithConnectionException() throws Exception {
    final Latch latch = new Latch();
    final List<ExceptionNotification> notifications = new ArrayList<>();
    final ExceptionNotificationListener listener = notification -> {
      notifications.add(notification);
      latch.release();
    };
    notificationListenerRegistry.registerListener(listener);
    try {
      startFlow("pet-tiger");
      latch.await(TIMEOUT, MILLISECONDS);
      assertThat(notifications.size(), greaterThanOrEqualTo(1));
      assertThat(notifications.get(0).getInfo(), notNullValue());
      assertThat(notifications.get(0).getInfo().getException(), notNullValue());
      assertThat(notifications.get(0).getInfo().getException(), instanceOf(RuntimeException.class));
      assertThat(notifications.get(0).getInfo().getException().getCause(), instanceOf(ConnectionException.class));
      assertThat(notifications.get(0).getInfo().getException().getCause().getMessage(), is("A tiger cannot be petted."));
      assertThat(notifications.get(0).getResourceIdentifier(), is("pet-tiger"));
    } finally {
      notificationListenerRegistry.unregisterListener(listener);
    }
  }

  @Test
  public void sourcePollFailWithException() throws Exception {
    final Latch latch = new Latch();
    final List<ExceptionNotification> notifications = new ArrayList<>();
    final ExceptionNotificationListener listener = notification -> {
      notifications.add(notification);
      latch.release();
    };
    notificationListenerRegistry.registerListener(listener);
    try {
      startFlow("pet-whale");
      latch.await(TIMEOUT, MILLISECONDS);
      assertThat(notifications.size(), greaterThanOrEqualTo(1));
      assertThat(notifications.get(0).getInfo(), notNullValue());
      assertThat(notifications.get(0).getInfo().getException(), notNullValue());
      assertThat(notifications.get(0).getInfo().getException(), instanceOf(RuntimeException.class));
      assertThat(notifications.get(0).getInfo().getException().getMessage(), is("Why do you want to pet a whale?"));
      assertThat(notifications.get(0).getResourceIdentifier(), is("pet-whale"));
    } finally {
      notificationListenerRegistry.unregisterListener(listener);
    }
  }

  @Test
  public void sourcePollReportConnectionException() throws Exception {
    final Latch latch = new Latch();
    final List<ExceptionNotification> notifications = new ArrayList<>();
    final ExceptionNotificationListener listener = notification -> {
      notifications.add(notification);
      latch.release();
    };
    notificationListenerRegistry.registerListener(listener);
    try {
      startFlow("pet-dinosaur");
      latch.await(TIMEOUT, MILLISECONDS);
      assertThat(notifications.size(), greaterThanOrEqualTo(1));
      assertThat(notifications.get(0).getInfo(), notNullValue());
      assertThat(notifications.get(0).getInfo().getException(), notNullValue());
      assertThat(notifications.get(0).getInfo().getException(), instanceOf(ConnectionException.class));
      assertThat(notifications.get(0).getInfo().getException().getMessage(), is("Dinosaurs no longer exist."));
      assertThat(notifications.get(0).getResourceIdentifier(), is("pet-dinosaur"));
    } finally {
      notificationListenerRegistry.unregisterListener(listener);
    }
  }

  @Test
  public void noExceptionNotificationSent() throws Exception {
    final Latch latch = new Latch();
    final List<ExceptionNotification> notifications = new ArrayList<>();
    final ExceptionNotificationListener listener = notification -> {
      notifications.add(notification);
      latch.release();
    };
    notificationListenerRegistry.registerListener(listener);
    try {
      startFlow("pet-dog");
      boolean timeout = !latch.await(TIMEOUT, MILLISECONDS);
      assertThat(timeout, is(true));
    } finally {
      notificationListenerRegistry.unregisterListener(listener);
    }
  }

  @Test
  public void PollingSourceNotification() throws Exception {
    final Latch latch2 = new Latch();
    final List<Notification> notifications2 = new ArrayList<>();
    final PipelineMessageNotificationListener listener2 = notification -> {
      notifications2.add(notification);
      if (valueOf(PROCESS_COMPLETE).equals(notification.getAction().getIdentifier())) {
        latch2.release();
      }
    };
    notificationListenerRegistry.registerListener(listener2);

    final Latch latch = new Latch();
    final List<PollingSourceItemNotification> notifications = new ArrayList<>();
    final PollingSourceItemNotificationListener listener = notification -> {
      notifications.add(notification);
      if (valueOf(ITEM_DISPATCHED).equals(notification.getAction().getIdentifier())) {
        latch.release();
      }
    };
    notificationListenerRegistry.registerListener(listener);
    try {
      startFlow("oneItemPoll");
      boolean timeout = !latch.await(50000, MILLISECONDS);
      boolean timeout2 = !latch2.await(50000, MILLISECONDS);
      assertThat(timeout, is(false));
      assertThat(timeout2, is(false));
      assertThat(notifications.isEmpty(), is(false));
      assertThat(notifications2.isEmpty(), is(false));
      assertThat(((PipelineMessageNotification) notifications2.get(0)).getEvent().getContext().getRootId(), is(notifications.get(0).getEventId()));
    } finally {
      notificationListenerRegistry.unregisterListener(listener);
    }
  }

  private void assertStartedPolls(int polls) {
    check(TIMEOUT, DELAY * 2, () -> {
      assertThat(STARTED_POLLS, is(polls));
      return true;
    });
  }

  private void assertPetFailingSourcePollsFromDifferentSources(int polls) {
    check(TIMEOUT, DELAY * 2, () -> {
      assertThat(PetFailingPollingSource.STARTED_POLLS, is(polls));
      return true;
    });
    assertThat(POLL_INVOCATIONS.size(), is(polls));
    POLL_INVOCATIONS.entrySet().forEach(entry -> assertThat(entry.getValue(), is(1)));
  }

  private void assertIdempotentAdoptions() {
    checkNot(TIMEOUT, DELAY, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() > ALL_PETS.size();
      }
    });
  }

  private void assertAllPetsAdopted() {
    check(TIMEOUT, DELAY * 2, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() >= ALL_PETS.size() &&
            ADOPTION_EVENTS.stream().map(e -> e.getMessage().getPayload().getValue().toString()).collect(toList())
                .containsAll(ALL_PETS);
      }
    });
  }

  private void assertAllNumbersAdoptedExactlyOnce() {
    check(TIMEOUT, DELAY * 2, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() == ALL_NUMBERS.size() &&
            ADOPTION_EVENTS.stream().map(e -> e.getMessage().getPayload().getValue().toString()).collect(toList())
                .containsAll(ALL_NUMBERS);
      }
    });
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

  private void stopFlow(String flowName) throws Exception {
    ((Stoppable) getFlowConstruct(flowName)).stop();
  }
}
