/**
 * 
 */
package org.mule.transport.sftp;

import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ServerNotification;

import java.util.concurrent.CountDownLatch;

/**
 * @author alejandrosequeira
 * Exception listener that decreases a latch when notified.
 */
@SuppressWarnings("rawtypes")
public class LatchDownExceptionListener implements ExceptionNotificationListener
{

    private final CountDownLatch latch;

    public LatchDownExceptionListener(CountDownLatch latch)
    {
        this.latch = latch;
    }

    /**
     * @see org.mule.api.context.notification.ServerNotificationListener#onNotification(org.mule.api.context.notification.ServerNotification)
     */
    @Override
    public void onNotification(ServerNotification notification)
    {
        latch.countDown();
    }

}
