/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.api.notification.PollingSourceItemNotification.ITEM_REJECTED_IDEMPOTENCY;
import static org.mule.runtime.api.notification.PollingSourceItemNotification.ITEM_REJECTED_WATERMARK;
import static org.mule.runtime.api.notification.PollingSourceNotification.POLL_FAILURE;
import static org.mule.runtime.api.notification.PollingSourceNotification.POLL_SUCCESS;
import static org.mule.runtime.api.util.MuleSystemProperties.EMIT_POLLING_SOURCE_NOTIFICATIONS;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.POLLING;
import static org.mule.test.petstore.extension.PetAdoptionSource.ALL_PETS;
import static org.mule.test.petstore.extension.WatermarkingPetAdoptionSource.resetSource;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.api.notification.PipelineMessageNotificationListener;
import org.mule.runtime.api.notification.PollingSourceItemNotification;
import org.mule.runtime.api.notification.PollingSourceItemNotificationListener;
import org.mule.runtime.api.notification.PollingSourceNotification;
import org.mule.runtime.api.notification.PollingSourceNotificationListener;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(SOURCES)
@Story(POLLING)
public class PollingSourceNotificationsTestCase extends AbstractExtensionFunctionalTestCase {

  private static final List<CoreEvent> ADOPTION_EVENTS = new LinkedList<>();
  private static final int TIMEOUT = 10000;
  private static final String POLL_STARTED_MESSAGE = "poll started";
  private static final String POLL_SUCCESS_MESSAGE = "poll successfully completed";
  private static final String POLL_FAILED_MESSAGE = "poll failed to complete";
  private static final String ITEM_DISPATCHED_MESSAGE = "item dispatched to flow";
  private static final String ITEM_REJECTED_IDEMPOTENCY_MESSAGE = "item rejected due to idempotency";
  private static final String ITEM_REJECTED_WATERMARK_MESSAGE = "item rejected due to watermark";

  @Rule
  public SystemProperty emitNotifications = new SystemProperty(EMIT_POLLING_SOURCE_NOTIFICATIONS, "true");

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
    return "source/polling-source-notifications-config.xml";
  }

  @Test
  public void pollSuccessNotifications() throws Exception {
    final Latch latch = new Latch();
    final List<PollingSourceNotification> sourceNotifications = new ArrayList<>();
    final PollingSourceNotificationListener sourceListener = notification -> {
      sourceNotifications.add(notification);
      if (valueOf(POLL_SUCCESS).equals(notification.getAction().getIdentifier())) {
        latch.release();
      }
    };
    notificationListenerRegistry.registerListener(sourceListener);

    try {
      startFlow("oneItemPoll");
      boolean timeout = !latch.await(TIMEOUT, MILLISECONDS);
      assertThat(timeout, is(false));
      assertThat(sourceNotifications.size(), is(2));
      assertThat(sourceNotifications.get(0).getActionName(), is(POLL_STARTED_MESSAGE));
      assertThat(sourceNotifications.get(1).getActionName(), is(POLL_SUCCESS_MESSAGE));
    } finally {
      notificationListenerRegistry.unregisterListener(sourceListener);
    }
  }

  @Test
  public void pollFailureNotifications() throws Exception {
    final Latch latch = new Latch();
    final List<PollingSourceNotification> sourceNotifications = new ArrayList<>();
    final PollingSourceNotificationListener sourceListener = notification -> {
      sourceNotifications.add(notification);
      if (valueOf(POLL_FAILURE).equals(notification.getAction().getIdentifier())) {
        latch.release();
      }
    };
    notificationListenerRegistry.registerListener(sourceListener);

    try {
      startFlow("pet-whale");
      boolean timeout = !latch.await(TIMEOUT, MILLISECONDS);
      assertThat(timeout, is(false));
      assertThat(sourceNotifications.size(), is(2));
      assertThat(sourceNotifications.get(0).getActionName(), is(POLL_STARTED_MESSAGE));
      assertThat(sourceNotifications.get(1).getActionName(), is(POLL_FAILED_MESSAGE));
    } finally {
      notificationListenerRegistry.unregisterListener(sourceListener);
    }
  }

  @Test
  public void matchEventIdFromNotifications() throws Exception {
    final Latch latch = new Latch();
    final List<Notification> pipelineNotifications = new ArrayList<>();
    final PipelineMessageNotificationListener pipelineListener = notification -> {
      pipelineNotifications.add(notification);
      if (valueOf(PROCESS_COMPLETE).equals(notification.getAction().getIdentifier())) {
        latch.release();
      }
    };
    notificationListenerRegistry.registerListener(pipelineListener);

    final List<PollingSourceItemNotification> sourceNotifications = new ArrayList<>();
    final PollingSourceItemNotificationListener sourceListener = sourceNotifications::add;
    notificationListenerRegistry.registerListener(sourceListener);

    try {
      startFlow("oneItemPoll");
      boolean timeout = !latch.await(TIMEOUT, MILLISECONDS);
      assertThat(timeout, is(false));
      assertThat(sourceNotifications.isEmpty(), is(false));
      assertThat(pipelineNotifications.isEmpty(), is(false));
      assertThat(((PipelineMessageNotification) pipelineNotifications.get(0)).getEvent().getContext().getRootId(),
                 is(sourceNotifications.get(0).getEventId().get()));
    } finally {
      notificationListenerRegistry.unregisterListener(pipelineListener);
      notificationListenerRegistry.unregisterListener(sourceListener);
    }
  }

  @Test
  public void itemIdempotencyNotifications() throws Exception {
    final Latch latch = new Latch();
    final List<PollingSourceItemNotification> sourceNotifications = new ArrayList<>();
    final PollingSourceItemNotificationListener sourceListener = notification -> {
      sourceNotifications.add(notification);
      if (valueOf(ITEM_REJECTED_IDEMPOTENCY).equals(notification.getAction().getIdentifier())
          && sourceNotifications.size() >= ALL_PETS.size() * 2) {
        latch.release();
      }
    };
    notificationListenerRegistry.registerListener(sourceListener);

    try {
      startFlow("idempotent");
      boolean timeout = !latch.await(TIMEOUT, MILLISECONDS);
      assertThat(timeout, is(false));
      assertThat(sourceNotifications.get(0).getActionName(), is(ITEM_DISPATCHED_MESSAGE));
      assertThat(sourceNotifications.get(ALL_PETS.size() - 1).getActionName(), is(ITEM_DISPATCHED_MESSAGE));
      assertThat(sourceNotifications.get(ALL_PETS.size()).getActionName(), is(ITEM_REJECTED_IDEMPOTENCY_MESSAGE));
      assertThat(sourceNotifications.get(ALL_PETS.size() * 2 - 1).getActionName(), is(ITEM_REJECTED_IDEMPOTENCY_MESSAGE));
    } finally {
      notificationListenerRegistry.unregisterListener(sourceListener);
    }
  }

  @Test
  public void itemWatermarkNotifications() throws Exception {
    final Latch latch = new Latch();
    final List<PollingSourceItemNotification> sourceNotifications = new ArrayList<>();
    final PollingSourceItemNotificationListener sourceListener = notification -> {
      sourceNotifications.add(notification);
      if (valueOf(ITEM_REJECTED_WATERMARK).equals(notification.getAction().getIdentifier())) {
        latch.release();
      }
    };
    notificationListenerRegistry.registerListener(sourceListener);

    try {
      startFlow("watermarkPoll");
      boolean timeout = !latch.await(TIMEOUT, MILLISECONDS);
      assertThat(timeout, is(false));
      assertThat(sourceNotifications.get(0).getActionName(), is(ITEM_DISPATCHED_MESSAGE));
      assertThat(sourceNotifications.get(1).getActionName(), is(ITEM_REJECTED_WATERMARK_MESSAGE));
    } finally {
      notificationListenerRegistry.unregisterListener(sourceListener);
    }
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }
}
