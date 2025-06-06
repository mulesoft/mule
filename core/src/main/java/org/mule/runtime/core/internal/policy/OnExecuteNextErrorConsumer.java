/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.api.notification.PolicyNotification.AFTER_NEXT;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.DefaultFlowCallStack;
import org.mule.runtime.core.privileged.exception.MessagingException;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.namespace.QName;

/**
 * Consumer executed when a chain triggered by a policy's execute-next processor finishes with an error. That chain can be a flow,
 * a single operation or other policies.
 */
public class OnExecuteNextErrorConsumer implements Consumer<Throwable> {

  private final Function<MessagingException, CoreEvent> prepareEvent;
  private final PolicyNotificationHelper notificationHelper;
  private final ComponentLocation location;
  private final Map<QName, Object> annotations;

  public OnExecuteNextErrorConsumer(Function<MessagingException, CoreEvent> prepareEvent,
                                    PolicyNotificationHelper notificationHelper,
                                    ComponentLocation location,
                                    Map<QName, Object> annotations) {
    this.prepareEvent = prepareEvent;
    this.notificationHelper = notificationHelper;
    this.location = location;
    this.annotations = annotations;
  }

  @Override
  public void accept(Throwable error) {
    MessagingException me = (MessagingException) error;

    CoreEvent newEvent = prepareEvent.apply(me);

    me.setProcessedEvent(newEvent);

    notificationHelper.fireNotification(newEvent, me, AFTER_NEXT);

    pushAfterNextFlowStackElement().accept(newEvent);
  }

  private Consumer<CoreEvent> pushAfterNextFlowStackElement() {
    return event -> ((DefaultFlowCallStack) event.getFlowCallStack())
        .push(new FlowStackElement(toPolicyLocation(location), null, location, annotations));
  }

  private String toPolicyLocation(ComponentLocation componentLocation) {
    return componentLocation.getParts().get(0).getPartPath() + "/" + componentLocation.getParts().get(1).getPartPath()
        + "[after next]";
  }

}
