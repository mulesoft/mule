/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.context.notification;

import org.mule.api.MuleMessage;

/**
 * Default implementation of {@link BlockingServerEvent} that guarantees all the notifications that extend this class
 * send a copy of the {@link MuleMessage} instead of the original one. This avoids race conditions for getting the message ownership.
 *
 * @since 3.10
 */
public abstract class AbstractBlockingServerEvent extends ServerNotification implements BlockingServerEvent
{


    public AbstractBlockingServerEvent(Object resource, int action, String resourceIdentifier)
    {
        super(resource, action, resourceIdentifier, true);
    }

    public AbstractBlockingServerEvent(Object resource, int action)
    {
        this(resource, action, null);
    }

}
