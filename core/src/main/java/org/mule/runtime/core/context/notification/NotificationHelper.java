/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context.notification;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple class to fire notifications of a specified type over a {@link ServerNotificationHandler}.
 *
 * When the notification is to be sent on the context of the processing of a {@link MuleEvent} (meaning, the method used to fire
 * the notification takes a {@link MuleEvent} argument), then this instance will delegate into a {@link ServerNotificationHandler}
 * that corresponds to the {@link MuleContext} of that event. When the notification does not relate to a particular
 * {@link MuleEvent} (for example, connection/reconnection/disconnection events), then a {@link #defaultNotificationHandler} will
 * be used
 */
public class NotificationHelper {

  private static final Logger logger = LoggerFactory.getLogger(NotificationHelper.class);

  private final Class<? extends ServerNotification> notificationClass;
  private final boolean dynamicNotifications;
  private final ServerNotificationHandler defaultNotificationHandler;
  private final LoadingCache<MuleContext, ServerNotificationHandler> serverNotificationHandlers =
      CacheBuilder.newBuilder().build(new CacheLoader<MuleContext, ServerNotificationHandler>() {

        @Override
        public ServerNotificationHandler load(MuleContext muleContext) throws Exception {
          return adaptNotificationHandler(muleContext.getNotificationManager());
        }
      });


  /**
   * Creates a new {@link NotificationHelper} that emits instances of {@code notificationClass} class.
   *
   * @param defaultNotificationHandler The {@link ServerNotificationHandler} to be used on notifications which don't relate to a
   *        {@link MuleEvent}
   * @param notificationClass The {@link Class} of the notifications to be fired by this helper
   * @param dynamicNotifications If {@code true}, notifications will be fired directly to a {@link ServerNotificationHandler}
   *        responsible to decide to emit it or not. If {@code false} the notification will be checked to be enable or not at
   *        creation time
   */
  public NotificationHelper(ServerNotificationHandler defaultNotificationHandler,
                            Class<? extends ServerNotification> notificationClass, boolean dynamicNotifications) {
    this.notificationClass = notificationClass;
    this.dynamicNotifications = dynamicNotifications;
    this.defaultNotificationHandler = adaptNotificationHandler(defaultNotificationHandler);
  }

  /**
   * Checks if the {@link #defaultNotificationHandler} is enabled to fire instances of {@link #notificationClass}. Use this method
   * when planning to fire a notification that is not related to a {@link MuleEvent} (connect/disconnect/etc). Otherwise, use
   * {@link #isNotificationEnabled(MuleEvent)} instead
   *
   * @return {@code true} if {@link #defaultNotificationHandler} is enabled for {@link #notificationClass}
   */
  public boolean isNotificationEnabled() {
    return defaultNotificationHandler.isNotificationEnabled(notificationClass);
  }

  /**
   * Checks if the {@link ServerNotificationHandler} associated to the given {@code event} is enabled to fire instances of
   * {@link #notificationClass}
   *
   * @return {@code true} if there is a {@link ServerNotificationHandler} enabled for {@link #notificationClass}
   */
  public boolean isNotificationEnabled(MuleEvent event) {
    return getNotificationHandler(event).isNotificationEnabled(notificationClass);
  }

  /**
   * Fires a {@link ConnectorMessageNotification} for the given arguments using the {@link ServerNotificationHandler} associated
   * to the given {@code event}
   *
   * @param source
   * @param event a {@link org.mule.runtime.core.api.MuleEvent}
   * @param uri the uri of the firing endpoint
   * @param flowConstruct the {@link org.mule.runtime.core.api.construct.FlowConstruct} that generated the notification
   * @param action the action code for the notification
   */
  public void fireNotification(Object source, MuleEvent event, String uri, FlowConstruct flowConstruct, int action) {
    doFireNotification(getNotificationHandler(event), source, event.getMessage(), uri, flowConstruct, action);
  }

  /**
   * Fires the given {@code notification} using the {@link #defaultNotificationHandler}. Use this method when the
   * {@code notification} is not related to any {@link MuleEvent} (for example, connect/disconnect/etc). Otherwise, use
   * {@link #fireNotification(ServerNotification, MuleEvent)} instead
   *
   * @param notification a {@link ServerNotification}
   */
  public void fireNotification(ServerNotification notification) {
    defaultNotificationHandler.fireNotification(notification);
  }

  /**
   * Fires the given {@code notification} using the {@link ServerNotificationHandler} that corresponds to the given {@code event}
   *
   * @param notification a {@link ServerNotification}
   */
  public void fireNotification(ServerNotification notification, MuleEvent event) {
    getNotificationHandler(event).fireNotification(notification);
  }

  private void doFireNotification(ServerNotificationHandler serverNotificationHandler, Object source, MuleMessage message,
                                  String uri, FlowConstruct flowConstruct, int action) {
    try {
      if (serverNotificationHandler.isNotificationEnabled(notificationClass)) {
        serverNotificationHandler.fireNotification(new ConnectorMessageNotification(source, message, uri, flowConstruct, action));
      }
    } catch (Exception e) {
      logger.warn("Could not fire notification. Action: " + action, e);
    }
  }


  private ServerNotificationHandler adaptNotificationHandler(ServerNotificationHandler serverNotificationHandler) {
    return dynamicNotifications ? serverNotificationHandler
        : new OptimisedNotificationHandler(serverNotificationHandler, notificationClass);
  }

  private ServerNotificationHandler getNotificationHandler(MuleEvent event) {
    return serverNotificationHandlers.getUnchecked(event.getMuleContext());
  }
}
