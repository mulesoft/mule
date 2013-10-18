/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.notification;

import org.mule.api.context.notification.CustomNotificationListener;

/**
 * <code>SftpTransportNotificationListener</code> is an observer interface that
 * objects can use to receive notifications about sftp operations such as put, get,
 * rename and delete being performed by the sftp transport.
 */
public interface SftpTransportNotificationListener extends CustomNotificationListener
{
    // no methods
}
