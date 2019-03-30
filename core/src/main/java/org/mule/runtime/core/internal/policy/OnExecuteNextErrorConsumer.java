/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.api.notification.PolicyNotification.AFTER_NEXT;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_NEXT_EVENT_CTX_IDS;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Consumer executed when a chain triggered by a policy's execute-next processor finishes with an error. That chain can be a flow,
 * a single operation or other policies.
 */
public class OnExecuteNextErrorConsumer implements Consumer<Throwable> {

  private final Function<CoreEvent, CoreEvent> prepareEvent;
  private final PolicyNotificationHelper notificationHelper;
  private final ComponentLocation location;

  public OnExecuteNextErrorConsumer(Function<CoreEvent, CoreEvent> prepareEvent,
                                    PolicyNotificationHelper notificationHelper,
                                    ComponentLocation location) {
    this.prepareEvent = prepareEvent;
    this.notificationHelper = notificationHelper;
    this.location = location;
  }

  @Override
  public void accept(Throwable error) {
    MessagingException me = (MessagingException) error;

    if (isEventContextHandledByThisNext(me.getEvent())) {
      CoreEvent newEvent = prepareEvent.apply(me.getEvent());

      me.setProcessedEvent(newEvent);

      notificationHelper.fireNotification(newEvent, me, AFTER_NEXT);

      pushAfterNextFlowStackElement().accept(newEvent);

      ((BaseEventContext) newEvent.getContext()).error(error);
    }
  }

  private Consumer<CoreEvent> pushAfterNextFlowStackElement() {
    return event -> ((DefaultFlowCallStack) event.getFlowCallStack())
        .push(new FlowStackElement(toPolicyLocation(location), null));
  }

  private String toPolicyLocation(ComponentLocation componentLocation) {
    return componentLocation.getParts().get(0).getPartPath() + "/" + componentLocation.getParts().get(1).getPartPath()
        + "[after next]";
  }

  private boolean isEventContextHandledByThisNext(CoreEvent event) {
    final Set<String> eventCtxIds = ((InternalEvent) event).getInternalParameter(POLICY_NEXT_EVENT_CTX_IDS);
    return eventCtxIds != null && eventCtxIds.contains(event.getContext().getId());
  }
}
