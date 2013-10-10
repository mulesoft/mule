/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
