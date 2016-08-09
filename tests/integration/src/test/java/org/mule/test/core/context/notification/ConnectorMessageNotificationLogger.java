/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.core.api.context.notification.ConnectorMessageNotificationListener;
import org.mule.runtime.core.context.notification.ConnectorMessageNotification;

public class ConnectorMessageNotificationLogger extends AbstractNotificationLogger<ConnectorMessageNotification>
    implements ConnectorMessageNotificationListener<ConnectorMessageNotification> {
  // nothing to do here
}
