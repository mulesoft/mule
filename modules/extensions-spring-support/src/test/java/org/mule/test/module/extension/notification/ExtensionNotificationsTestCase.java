/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.notification;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.notification.ExtensionNotification;
import org.mule.runtime.api.notification.ExtensionNotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.heisenberg.extension.model.SimpleKnockeableDoor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.Test;

public class ExtensionNotificationsTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String HEISENBERG = HeisenbergExtension.HEISENBERG.toUpperCase();
  private static final String NEW_BATCH = "NEW_BATCH";
  private static final String NEXT_BATCH = "NEXT_BATCH";
  private static final String BATCH_TERMINATED = "BATCH_TERMINATED";
  private static final String BATCH_DELIVERY_FAILED = "BATCH_DELIVERY_FAILED";
  private static final String BATCH_DELIVERED = "BATCH_DELIVERED";
  private static final String BATCH_FAILED = "BATCH_FAILED";
  private static final String KNOCKING_DOOR = "KNOCKING_DOOR";
  private static final String KNOCKED_DOOR = "KNOCKED_DOOR";

  private TestExtensionNotificationListener listener = null;

  @Inject
  private NotificationListenerRegistry notificationListenerRegistry;

  @Override
  protected String getConfigFile() {
    return "notifications-config.xml";
  }

  @Test
  public void operationFiresNotificationsWithCustomData() throws Exception {
    Latch latch = new Latch();
    setUpListener(notification -> checkIfDone(latch, 2), false);

    String correlationId = flowRunner("operationNotification").run().getCorrelationId();

    assertThat("Expected notifications not received.", latch.await(6000, MILLISECONDS), is(true));

    MultiMap<String, ExtensionNotification> notifications = listener.getNotifications();
    Set<String> keys = notifications.keySet();
    assertThat(keys, hasItem(KNOCKING_DOOR));
    assertThat(keys, hasItem(KNOCKED_DOOR));

    ExtensionNotification knockingDoor = notifications.get(KNOCKING_DOOR);
    assertThat(knockingDoor, is(notNullValue()));
    assertThat(knockingDoor.getAction().getNamespace(), is(HEISENBERG));
    assertThat(knockingDoor.getData().getValue(), instanceOf(SimpleKnockeableDoor.class));
    assertThat(((SimpleKnockeableDoor) knockingDoor.getData().getValue()).getSimpleName(),
               is("Top Level Skyler @ 308 Negra Arroyo Lane"));
    assertThat(knockingDoor.getEvent().getCorrelationId(), is(correlationId));

    ExtensionNotification knockedDoor = notifications.get(KNOCKED_DOOR);
    assertThat(knockedDoor, is(notNullValue()));
    assertThat(knockedDoor.getAction().getNamespace(), is(HEISENBERG));
    assertThat(knockedDoor.getData().getValue(), instanceOf(SimpleKnockeableDoor.class));
    assertThat(((SimpleKnockeableDoor) knockedDoor.getData().getValue()).getSimpleName(),
               is("Top Level Skyler @ 308 Negra Arroyo Lane"));
    assertThat(knockedDoor.getEvent().getCorrelationId(), is(correlationId));
  }

  @Test
  public void sourceFiresNotificationsOnSuccess() throws Exception {
    Latch latch = new Latch();
    setUpListener(notification -> checkIfDone(latch, 4), false);

    Flow flow = (Flow) getFlowConstruct("sourceNotifications");
    flow.start();

    assertThat("Expected notifications not received.", latch.await(6000, MILLISECONDS), is(true));

    Map<String, ExtensionNotification> notifications = listener.getNotifications();
    Set<String> keys = notifications.keySet();
    assertThat(keys, hasItem(NEW_BATCH));
    assertThat(keys, hasItem(NEXT_BATCH));
    assertThat(keys, hasItem(BATCH_DELIVERED));
    assertThat(keys, hasItem(BATCH_TERMINATED));

    ExtensionNotification newBatch = verifyNotificationAndValue(notifications.get(NEW_BATCH), 1);

    String correlationId = newBatch.getEvent().getCorrelationId();

    ExtensionNotification nextBatch = verifyNotificationAndValue(notifications.get(NEXT_BATCH), 1000000L);
    assertThat(nextBatch.getEvent().getCorrelationId(), is(correlationId));

    ExtensionNotification batchRedelivered = verifyNotificationAndValue(notifications.get(BATCH_DELIVERED), 100L);
    assertThat(batchRedelivered.getEvent().getCorrelationId(), is(correlationId));

    ExtensionNotification batchTerminated = verifyNotificationAndValue(notifications.get(BATCH_TERMINATED), 1);
    assertThat(batchTerminated.getEvent().getCorrelationId(), is(correlationId));
  }

  @Test
  public void sourceFiresNotificationsOnError() throws Exception {
    Latch latch = new Latch();
    setUpListener(notification -> checkIfDone(latch, 4), false);

    Flow flow = (Flow) getFlowConstruct("sourceNotificationsError");
    flow.start();

    assertThat("Expected notifications not received", latch.await(6000, MILLISECONDS), is(true));

    MultiMap<String, ExtensionNotification> notifications = listener.getNotifications();
    Set<String> keys = notifications.keySet();
    assertThat(keys, hasItem(NEW_BATCH));
    assertThat(keys, hasItem(NEXT_BATCH));
    assertThat(keys, hasItem(BATCH_DELIVERY_FAILED));
    assertThat(keys, hasItem(BATCH_TERMINATED));

    ExtensionNotification newBatch = verifyNotificationAndValue(notifications.get(NEW_BATCH), 1);

    String correlationId = newBatch.getEvent().getCorrelationId();

    ExtensionNotification nextBatch = verifyNotificationAndValue(notifications.get(NEXT_BATCH), 1000000L);
    assertThat(nextBatch.getEvent().getCorrelationId(), is(correlationId));

    ExtensionNotification batchDeliveryFailed = notifications.get(BATCH_DELIVERY_FAILED);
    assertThat(batchDeliveryFailed, is(notNullValue()));
    assertThat(batchDeliveryFailed.getAction().getNamespace(), is(HEISENBERG));
    assertThat(batchDeliveryFailed.getData().getValue(), instanceOf(PersonalInfo.class));
    assertThat(((PersonalInfo) batchDeliveryFailed.getData().getValue()).getAge(), is(27));
    assertThat(batchDeliveryFailed.getEvent().getCorrelationId(), is(correlationId));

    ExtensionNotification batchTerminated = verifyNotificationAndValue(notifications.get(BATCH_TERMINATED), 1);
    assertThat(batchTerminated.getEvent().getCorrelationId(), is(correlationId));
  }

  @Test
  public void sourceFiresNotificationsOnBackPressure() throws Exception {
    Latch failed = new Latch();
    final Reference<ExtensionNotification> batchFailed = new Reference<>();

    setUpListener(notification -> {
      if (BATCH_FAILED.equals(notification.getAction().getIdentifier())) {
        batchFailed.set(notification);
        failed.release();
      }
    }, true);

    Flow flow = (Flow) getFlowConstruct("sourceNotificationsBackPressure");
    flow.start();

    assertThat("Batch failure notification not received.", failed.await(10000, MILLISECONDS), is(true));

    ExtensionNotification backPressureNotification = batchFailed.get();
    assertThat(backPressureNotification, is(notNullValue()));
    String correlationId = backPressureNotification.getEvent().getCorrelationId();

    new PollingProber(10000, 200).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return listener.getNotifications().getAll(BATCH_FAILED).stream()
            .anyMatch(n -> n.getEvent().getCorrelationId().equals(correlationId));
      }

      @Override
      public String describeFailure() {
        return "Expected notifications not found.";
      }
    });
    flow.stop();

    MultiMap<String, ExtensionNotification> notifications = listener.getNotifications();
    Set<String> keys = notifications.keySet();
    assertThat(keys, hasItem(NEW_BATCH));
    assertThat(keys, hasItem(NEXT_BATCH));
    assertThat(keys, hasItem(BATCH_FAILED));
    assertThat(keys, hasItem(BATCH_TERMINATED));

    int batchNumber = (Integer) backPressureNotification.getData().getValue();

    verifyNotificationAndValue(backPressureNotification, batchNumber);
    verifyNotificationAndValue(getNotificationMatch(notifications, correlationId, BATCH_TERMINATED), batchNumber);
  }

  private void checkIfDone(Latch latch, int expectedKeys) {
    if (listener.getNotifications().keySet().size() == expectedKeys) {
      latch.release();
    }
  }

  private ExtensionNotification getNotificationMatch(MultiMap<String, ExtensionNotification> notifications, String correlation,
                                                     String id) {
    return notifications.getAll(id).stream()
        .filter(n -> correlation.equals(n.getEvent().getCorrelationId()))
        .findAny()
        .orElse(null);
  }

  private <T> ExtensionNotification verifyNotificationAndValue(ExtensionNotification notification, T expected) {
    assertThat(notification.getAction().getNamespace(), is(HEISENBERG));
    assertThat(notification.getData().getValue(), instanceOf(expected.getClass()));
    assertThat(notification.getData().getValue(), is(expected));
    return notification;
  }

  private void setUpListener(Consumer<ExtensionNotification> onNotification, boolean correlationOn) {
    listener = new TestExtensionNotificationListener(onNotification, correlationOn);
    notificationListenerRegistry.registerListener(listener);
  }

  private class TestExtensionNotificationListener implements ExtensionNotificationListener {

    private Consumer<ExtensionNotification> onNotification;
    private MultiMap<String, ExtensionNotification> notifications = new MultiMap<>();
    private Map<String, Integer> correlationCount;

    public TestExtensionNotificationListener(Consumer<ExtensionNotification> onNotification, boolean correlationOn) {
      this.onNotification = onNotification;
      if (correlationOn) {
        correlationCount = new HashMap<>();
      }
    }

    @Override
    public synchronized void onNotification(ExtensionNotification notification) {
      notifications.put(notification.getAction().getIdentifier(), notification);
      if (correlationCount != null) {
        String correlationId = notification.getEvent().getCorrelationId();
        correlationCount.put(correlationId, correlationCount.computeIfAbsent(correlationId, correlation -> 0) + 1);
      }
      onNotification.accept(notification);
    }

    public MultiMap<String, ExtensionNotification> getNotifications() {
      return notifications;
    }

    public synchronized Integer getCorrelationCount(String correlationId) {
      return correlationCount.get(correlationId);
    }

  }

}
