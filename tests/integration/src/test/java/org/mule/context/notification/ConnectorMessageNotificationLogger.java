/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ConnectorMessageNotificationListener;

public class ConnectorMessageNotificationLogger extends AbstractNotificationLogger<ConnectorMessageNotification>
    implements ConnectorMessageNotificationListener<ConnectorMessageNotification>
{
    // nothing to do here
}
