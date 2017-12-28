/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.notification;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.notification.ExtensionNotification;
import org.mule.runtime.api.notification.ExtensionNotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.heisenberg.extension.model.SimpleKnockeableDoor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.Test;

public class ExtensionNotificationsTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String HEISENBERG = HeisenbergExtension.HEISENBERG.toUpperCase();

  private TestExtensionNotificationListener listener;

  @Inject
  private NotificationListenerRegistry notificationListenerRegistry;

  @Override
  protected String getConfigFile() {
    return "notifications-config.xml";
  }

  @Test
  public void operationFiresNotificationsWithCustomData() throws Exception {
    CountDownLatch latch = new CountDownLatch(2);
    setUpListener(notification -> latch.countDown());

    String correlationId = flowRunner("operationNotification").run().getCorrelationId();

    latch.await(2000, MILLISECONDS);

    assertThat(listener.getNotifications(), hasSize(2));

    ExtensionNotification notification1 = listener.getNotifications().get(0);
    assertThat(notification1.getAction().getNamespace(), is(HEISENBERG));
    assertThat(notification1.getAction().getIdentifier(), is("KNOCKING_DOOR"));
    assertThat(notification1.getData().getValue(), instanceOf(SimpleKnockeableDoor.class));
    assertThat(((SimpleKnockeableDoor) notification1.getData().getValue()).getSimpleName(),
               is("Top Level Skyler @ 308 Negra Arroyo Lane"));
    assertThat(notification1.getEvent().getCorrelationId(), is(correlationId));

    ExtensionNotification notification2 = listener.getNotifications().get(1);
    assertThat(notification2.getAction().getNamespace(), is(HEISENBERG));
    assertThat(notification2.getAction().getIdentifier(), is("KNOCKED_DOOR"));
    assertThat(notification2.getData().getValue(), instanceOf(SimpleKnockeableDoor.class));
    assertThat(((SimpleKnockeableDoor) notification2.getData().getValue()).getSimpleName(),
               is("Top Level Skyler @ 308 Negra Arroyo Lane"));
    assertThat(notification2.getEvent().getCorrelationId(), is(correlationId));
  }

  @Test
  public void sourceFiresNotificationsOnSuccess() throws Exception {
    CountDownLatch latch = new CountDownLatch(4);
    setUpListener(notification -> latch.countDown());

    Flow flow = (Flow) getFlowConstruct("sourceNotifications");
    flow.start();

    latch.await(4000, MILLISECONDS);

    assertThat(listener.getNotifications(), hasSize(4));

    ExtensionNotification notification1 = listener.getNotifications().get(0);
    verifyNewBatch(notification1, 1);

    String correlationId = notification1.getEvent().getCorrelationId();

    ExtensionNotification notification2 = listener.getNotifications().get(1);
    verifyNextBatch(notification2, 100000L);
    assertThat(notification2.getEvent().getCorrelationId(), is(correlationId));

    ExtensionNotification notification3 = listener.getNotifications().get(2);
    verifyNotificationAndValue(notification3, "BATCH_DELIVERED", 100L);
    assertThat(notification3.getEvent().getCorrelationId(), is(correlationId));

    ExtensionNotification notification4 = listener.getNotifications().get(3);
    verifyBatchTerminated(notification4, 1);
    assertThat(notification4.getEvent().getCorrelationId(), is(correlationId));
  }

  @Test
  public void sourceFiresNotificationsOnError() throws Exception {
    CountDownLatch latch = new CountDownLatch(4);
    setUpListener(notification -> latch.countDown());

    Flow flow = (Flow) getFlowConstruct("sourceNotificationsError");
    flow.start();

    latch.await(4000, MILLISECONDS);

    assertThat(listener.getNotifications(), hasSize(4));

    ExtensionNotification notification1 = listener.getNotifications().get(0);
    verifyNewBatch(notification1, 1);

    String correlationId = notification1.getEvent().getCorrelationId();

    ExtensionNotification notification2 = listener.getNotifications().get(1);
    verifyNextBatch(notification2, 100000L);
    assertThat(notification2.getEvent().getCorrelationId(), is(correlationId));

    ExtensionNotification notification3 = listener.getNotifications().get(2);
    assertThat(notification3.getAction().getNamespace(), is(HEISENBERG));
    assertThat(notification3.getAction().getIdentifier(), is("BATCH_DELIVERY_FAILED"));
    assertThat(notification3.getData().getValue(), instanceOf(PersonalInfo.class));
    assertThat(((PersonalInfo) notification3.getData().getValue()).getAge(), is(27));
    assertThat(notification3.getEvent().getCorrelationId(), is(correlationId));

    ExtensionNotification notification4 = listener.getNotifications().get(3);
    verifyBatchTerminated(notification4, 1);
    assertThat(notification4.getEvent().getCorrelationId(), is(correlationId));
  }

  @Test
  public void sourceFiresNotificationsOnBackPressure() throws Exception {
    Latch latch = new Latch();
    String batchFailed = "BATCH_FAILED";

    setUpListener(notification -> {
      if (batchFailed.equals(notification.getAction().getIdentifier())) {
        latch.release();
      }
    });

    Flow flow = (Flow) getFlowConstruct("sourceNotificationsBackPressure");
    flow.start();

    latch.await(10000, MILLISECONDS);
    flow.stop();

    assertThat(listener.getNotifications(), hasSize(greaterThan(3)));

    //Find first BATCH_FAILED
    ExtensionNotification backPressureNotification = listener.getNotifications()
        .stream()
        .filter(n -> batchFailed.equals(n.getAction().getIdentifier()))
        .findFirst().get();
    //Find matching event notifications
    List<ExtensionNotification> notifications = listener.getNotifications()
        .stream()
        .filter(n -> backPressureNotification.getEvent().getCorrelationId().equals(n.getEvent().getCorrelationId()))
        .collect(toList());

    assertThat(notifications, hasSize(4));

    int batchNumber = (Integer) backPressureNotification.getData().getValue();

    ExtensionNotification notification1 = notifications.get(0);
    verifyNewBatch(notification1, batchNumber);

    ExtensionNotification notification2 = notifications.get(1);
    verifyNextBatch(notification2, 10L);

    ExtensionNotification notification3 = notifications.get(2);
    verifyNotificationAndValue(notification3, batchFailed, batchNumber);

    ExtensionNotification notification4 = notifications.get(3);
    verifyBatchTerminated(notification4, batchNumber);
  }

  private void verifyNewBatch(ExtensionNotification notification, Integer expected) {
    verifyNotificationAndValue(notification, "NEW_BATCH", expected);
  }

  private void verifyNextBatch(ExtensionNotification notification, Long expected) {
    verifyNotificationAndValue(notification, "NEXT_BATCH", expected);
  }

  private void verifyBatchTerminated(ExtensionNotification notification, int expected) {
    verifyNotificationAndValue(notification, "BATCH_TERMINATED", expected);
  }

  private <T> void verifyNotificationAndValue(ExtensionNotification notification, String id, T expected) {
    assertThat(notification.getAction().getNamespace(), is(HEISENBERG));
    assertThat(notification.getAction().getIdentifier(), is(id));
    assertThat(notification.getData().getValue(), instanceOf(expected.getClass()));
    assertThat(notification.getData().getValue(), is(expected));
  }

  private void setUpListener(Consumer<ExtensionNotification> onNotification) {
    listener = new TestExtensionNotificationListener(onNotification);
    notificationListenerRegistry.registerListener(listener);
  }

  private class TestExtensionNotificationListener implements ExtensionNotificationListener {

    private Consumer<ExtensionNotification> onNotification;
    private List<ExtensionNotification> notifications = new LinkedList<>();

    public TestExtensionNotificationListener(Consumer<ExtensionNotification> onNotification) {
      this.onNotification = onNotification;
    }

    @Override
    public void onNotification(ExtensionNotification notification) {
      notifications.add(notification);
      onNotification.accept(notification);
    }

    public List<ExtensionNotification> getNotifications() {
      return notifications;
    }

  }

}
