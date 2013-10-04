/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
