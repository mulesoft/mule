/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static org.mule.runtime.core.privileged.event.PrivilegedEvent.getCurrentEvent;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.NotificationHelper;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Provides a reusable way for concrete {@link MessageProcessPhase}s to fire notifications.
 */
public abstract class NotificationFiringProcessingPhase<Template extends MessageProcessTemplate>
    implements MessageProcessPhase<Template>, Comparable<MessageProcessPhase> {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private NotificationHelper notificationHelper;
  protected MuleContext muleContext;

  protected void fireNotification(Component source, CoreEvent event, FlowConstruct flow, int action) {
    try {
      if (event == null) {
        // Null result only happens when there's a filter in the chain.
        // Unfortunately a filter causes the whole chain to return null
        // and there's no other way to retrieve the last event but using the RequestContext.
        // see https://www.mulesoft.org/jira/browse/MULE-8670
        event = getCurrentEvent();
        if (event == null) {
          return;
        }
      }
      notificationHelper.fireNotification(source, event, ((Component) flow).getLocation(), action);
    } catch (Exception e) {
      logger.warn("Could not fire notification. Action: " + action, e);
    }
  }

  @Inject
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    this.notificationHelper =
        new NotificationHelper(muleContext.getNotificationManager(), ConnectorMessageNotification.class, false);
  }
}
