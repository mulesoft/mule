/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.context.notification;

import org.mule.compatibility.core.api.context.notification.EndpointMessageNotificationListener;
import org.mule.compatibility.core.context.notification.EndpointMessageNotification;
import org.mule.runtime.core.context.notification.AbstractNotificationLogger;

public class EndpointMessageNotificationLogger extends AbstractNotificationLogger<EndpointMessageNotification>
    implements EndpointMessageNotificationListener<EndpointMessageNotification>
{
    // nothing to do here
}
