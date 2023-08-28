/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.api.notification;

import org.mule.functional.api.component.FunctionalTestProcessor;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.api.notification.CustomNotificationListener;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * By implementing this listener interface and registering the object with the
 * {@link NotificationListenerRegistry#registerListener(NotificationListener)} You can receive {@link FunctionalTestNotification}s
 * from the {@link FunctionalTestProcessor}.
 *
 * This Notification contains the current {@link CoreEvent}, {@link FlowConstruct} and reply message. The resource identifier for
 * this event is the service name that received the message. This means you can register to listen to Notifications from a
 * selected {@link FunctionalTestProcessor}. i.e. <code>
 * muleContext.registerListener(this, "*JmsTestCompoennt");
 * </code>
 *
 * This registration would only receive {@link FunctionalTestNotification} objects from components called 'MyJmsTestComponent' and
 * 'YourJmsTestComponent' but not 'HerFileTestComponent'.
 *
 * To receive all notifications simply do - <code>
 * muleContext.registerListener(this");
 * </code>
 *
 * @see FunctionalTestProcessor
 * @see FunctionalTestNotificationListener
 * @see MuleContext
 */
public interface FunctionalTestNotificationListener extends CustomNotificationListener<FunctionalTestNotification> {

  @Override
  default boolean isBlocking() {
    return false;
  }
}
