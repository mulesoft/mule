/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.notification;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.notification.ExtensionNotification;
import org.mule.runtime.api.notification.ExtensionNotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.SimpleKnockeableDoor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import org.junit.Test;

public class ExtensionNotificationsTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String HEISENBERG = HeisenbergExtension.HEISENBERG.toUpperCase();

  private CountDownLatch latch;
  private TestExtensionNotificationsListener listener;

  @Inject
  private NotificationListenerRegistry notificationListenerRegistry;

  @Override
  protected String getConfigFile() {
    return "notifications-config.xml";
  }

  @Test
  public void operationFiresNotificationsWithCustomData() throws Exception {
    setUpListener(2);

    flowRunner("operationNotification").run();

    latch.await(2000, MILLISECONDS);

    assertThat(listener.getNotifications(), hasSize(2));
    listener.getNotifications().forEach(notification -> {
      assertThat(notification.getAction().getNamespace(), is(HEISENBERG));
      assertThat(notification.getData().getValue(), instanceOf(SimpleKnockeableDoor.class));
      assertThat(((SimpleKnockeableDoor) notification.getData().getValue()).getSimpleName(),
                 is("Top Level Skyler @ 308 Negra Arroyo Lane"));
    });
    assertThat(listener.getNotifications().get(0).getAction().getId(), is("KNOCKING_DOOR"));
    assertThat(listener.getNotifications().get(1).getAction().getId(), is("KNOCKED_DOOR"));
  }

  @Test
  public void sourceFiresNotifications() throws Exception {
    setUpListener(2);

    Flow flow = (Flow) getFlowConstruct("sourceNotifications");
    flow.start();

    latch.await(2000, MILLISECONDS);

    assertThat(listener.getNotifications(), hasSize(2));

    ExtensionNotification firstNotification = listener.getNotifications().get(0);
    assertThat(firstNotification.getAction().getNamespace(), is(HEISENBERG));
    assertThat(firstNotification.getAction().getId(), is("NEW_BATCH"));
    assertThat(firstNotification.getData().getValue(), instanceOf(Integer.class));
    assertThat(firstNotification.getData().getValue(), is(1));

    ExtensionNotification secondNotification = listener.getNotifications().get(1);
    assertThat(secondNotification.getAction().getNamespace(), is(HEISENBERG));
    assertThat(secondNotification.getAction().getId(), is("BATCH_DELIVERED"));
    assertThat(secondNotification.getData().getValue(), instanceOf(Long.class));
    assertThat(secondNotification.getData().getValue(), is(100L));
  }

  private void setUpListener(int count) {
    latch = new CountDownLatch(count);
    listener = new TestExtensionNotificationsListener(latch);
    notificationListenerRegistry.registerListener(listener);
  }

  private class TestExtensionNotificationsListener implements ExtensionNotificationListener {

    private CountDownLatch latch;
    private List<ExtensionNotification> notifications = new LinkedList<>();

    public TestExtensionNotificationsListener(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void onNotification(ExtensionNotification notification) {
      notifications.add(notification);
      latch.countDown();
    }

    public List<ExtensionNotification> getNotifications() {
      return notifications;
    }

  }

}
