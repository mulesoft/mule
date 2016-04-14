/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.context.notification;

import org.mule.context.notification.SecurityNotification;


/**
 * <code>MuleContextNotificationListener</code> is an observer interface that objects
 * can implement to receive notifications about secure access requests.
 */
public interface SecurityNotificationListener<T extends SecurityNotification> extends ServerNotificationListener<SecurityNotification>
{
    // no methods
}
