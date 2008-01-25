/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.functional;

import org.mule.api.context.notification.CustomNotificationListener;

/**
 * By implementing this listener interface and registering the object with the
 * {@link org.mule.api.MuleContext#registerListener(org.mule.api.context.ServerNotificationListener)}
 * You can receive {@link org.mule.tck.functional.FunctionalTestNotification}s from the
 * {@link org.mule.tck.functional.FunctionalTestComponent}.
 *
 * This Notification contains the current MuleEventContext and reply message. The resource Identifier for this event
 * is the service name that received the message.  This means you can register to listen to Notifications from a
 * selected {@link org.mule.tck.functional.FunctionalTestComponent}. i.e.
 * <code>
 * muleContext.registerListener(this, "*JmsTestCompoennt");
 * </code>
 *
 * This registration would only receive {@link org.mule.tck.functional.FunctionalTestNotification} objects
 * from components called 'MyJmsTestComponent' and 'YourJmsTestComponent' but not 'HerFileTestComponent'.
 *
 * To receive all notifications simply do -
 * <code>
 * muleContext.registerListener(this");
 * </code>
 *
 * @see org.mule.tck.functional.FunctionalTestComponent
 * @see org.mule.tck.functional.FunctionalTestNotificationListener
 * @see org.mule.api.MuleContext
 */
public interface FunctionalTestNotificationListener extends CustomNotificationListener
{
    // no methods
}
