/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.context.notification;

import org.mule.compatibility.core.context.notification.EndpointMessageNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;

/**
 * <code>EndpointMessageNotificationListener</code> is an observer interface that objects can use to receive notifications about
 * messages being sent and received from endpoints
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface EndpointMessageNotificationListener<T extends EndpointMessageNotification>
    extends ServerNotificationListener<EndpointMessageNotification> {
  // no methods
}
