/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;

/**
 * An implementation detail - this enforces the guarantee that once
 * {@link Node#serial(RestrictedNode)} is called, {@link Node#parallel(RestrictedNode)} cannot be.
 */
public interface RestrictedNode
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
