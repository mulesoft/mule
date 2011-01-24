
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
