/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;

/**
 * An implementation detail - this enforces the guarantee that once
 * {@link Node#serial(RestrictedNode)} is called, {@link Node#parallel(RestrictedNode)} cannot be.
 */
interface RestrictedNode 
{

    public RestrictedNode serial(RestrictedNode node);

    public int match(ServerNotification notification);

    public boolean contains(Class clazz, int action);

    public boolean isExhausted();

    /**
     * @return Any remaining node
     */
    public RestrictedNode getAnyRemaining();

    Class getNotificationClass();

}
