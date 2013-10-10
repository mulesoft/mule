/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.functional;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.CustomNotificationListener;
import org.mule.api.context.notification.ServerNotificationListener;

/**
 * By implementing this listener interface and registering the object with the
 * {@link MuleContext#registerListener(ServerNotificationListener)}
 * You can receive {@link FunctionalTestNotification}s from the {@link FunctionalTestComponent}.
 *
 * This Notification contains the current MuleEventContext and reply message. The resource 
 * identifier for this event is the service name that received the message.  This means you can 
 * register to listen to Notifications from a selected {@link FunctionalTestComponent}. i.e.
 * <code>
 * muleContext.registerListener(this, "*JmsTestCompoennt");
 * </code>
 *
 * This registration would only receive {@link FunctionalTestNotification} objects from components 
 * called 'MyJmsTestComponent' and 'YourJmsTestComponent' but not 'HerFileTestComponent'.
 *
 * To receive all notifications simply do -
 * <code>
 * muleContext.registerListener(this");
 * </code>
 *
 * @see FunctionalTestComponent
 * @see FunctionalTestNotificationListener
 * @see MuleContext
 */
public interface FunctionalTestNotificationListener extends CustomNotificationListener
{
    // no methods
}
