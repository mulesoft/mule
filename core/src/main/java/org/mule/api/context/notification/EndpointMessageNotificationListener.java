/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.context.notification;

import org.mule.context.notification.EndpointMessageNotification;

/**
 * <code>EndpointMessageNotificationListener</code> is an observer interface that objects
 * can use to receive notifications about messages being sent and received from endpoints
 */
public interface EndpointMessageNotificationListener<T extends EndpointMessageNotification> extends ServerNotificationListener<EndpointMessageNotification>
{
    // no methods
}
