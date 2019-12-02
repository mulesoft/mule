/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.notification.PolicyNotification.AFTER_NEXT;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.LocationPart;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

public class OnExecuteNextErrorConsumerTestCase extends AbstractMuleTestCase {

  private static final String EVENT_ID = "eventId";

  private OnExecuteNextErrorConsumer consumer;

  private InternalEvent initialEvent;
  private InternalEvent updatedEvent;
  private PolicyNotificationHelper notificationHelper;
  private ComponentLocation location;
  private DefaultFlowCallStack flowCallStack;

  @Before
  public void setUp() {
    initialEvent = mock(InternalEvent.class, RETURNS_DEEP_STUBS);
    updatedEvent = mock(InternalEvent.class, RETURNS_DEEP_STUBS);
    notificationHelper = mock(PolicyNotificationHelper.class);
    location = mock(ComponentLocation.class);
    flowCallStack = mock(DefaultFlowCallStack.class);
    Function<CoreEvent, CoreEvent> prepareEvent = event -> updatedEvent;

    consumer = new OnExecuteNextErrorConsumer(prepareEvent, notificationHelper, location);
  }

  @Test
  public void notificationWithUpdatedEevenIsFired() {
    MessagingException messagingException = mock(MessagingException.class);

    when(initialEvent.getContext().getId()).thenReturn(EVENT_ID);
    when(updatedEvent.getFlowCallStack()).thenReturn(flowCallStack);
    when(messagingException.getEvent()).thenReturn(initialEvent);
    when(location.getParts()).thenReturn(newArrayList(mock(LocationPart.class), mock(LocationPart.class)));

    consumer.accept(messagingException);

    verify(notificationHelper).fireNotification(updatedEvent, messagingException, AFTER_NEXT);
    verify(flowCallStack).push(any());
  }
}
