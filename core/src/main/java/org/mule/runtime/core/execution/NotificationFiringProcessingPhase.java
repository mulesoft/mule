/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static org.mule.runtime.core.api.Event.getCurrentEvent;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.context.notification.ConnectorMessageNotification;
import org.mule.runtime.core.context.notification.NotificationHelper;
import org.mule.runtime.core.context.notification.ServerNotificationManager;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a reusable way for concrete {@link MessageProcessPhase}s to fire notifications.
 */
public abstract class NotificationFiringProcessingPhase<Template extends MessageProcessTemplate>
    implements MessageProcessPhase<Template>, Comparable<MessageProcessPhase>, MuleContextAware {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private ConcurrentHashMap<ServerNotificationManager, NotificationHelper> notificationHelpers = new ConcurrentHashMap<>();

  protected MuleContext muleContext;

  protected void fireNotification(Object source, Event event, FlowConstruct flow, int action) {
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
      getNotificationHelper(muleContext.getNotificationManager())
          .fireNotification(source, event, flow, action);
    } catch (Exception e) {
      logger.warn("Could not fire notification. Action: " + action, e);
    }
  }

  protected NotificationHelper getNotificationHelper(ServerNotificationManager serverNotificationManager) {
    NotificationHelper notificationHelper = notificationHelpers.get(serverNotificationManager);
    if (notificationHelper == null) {
      notificationHelper = new NotificationHelper(serverNotificationManager, ConnectorMessageNotification.class, false);
      notificationHelpers.putIfAbsent(serverNotificationManager, notificationHelper);
    }
    return notificationHelper;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;

  }
}
