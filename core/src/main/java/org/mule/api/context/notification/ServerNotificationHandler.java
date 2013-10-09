/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.context.notification;


public interface ServerNotificationHandler
{
    void fireNotification(ServerNotification notification);

    boolean isNotificationDynamic();

    /**
     * @since 3.0
     */
    boolean isListenerRegistered(ServerNotificationListener listener);

    /**
     * This returns a very "conservative" value - it is true if the notification or any subclass would be
     * accepted.  So if it returns false then you can be sure that there is no need to send the
     * notification.  On the other hand, if it returns true there is no guarantee that the notification
     * "really" will be dispatched to any listener.
     *
     * @param notfnClass Either the notification class being generated or some superclass
     * @return false if there is no need to dispatch the notification
     */
    boolean isNotificationEnabled(Class<? extends ServerNotification> notfnClass);

}
