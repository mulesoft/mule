/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.context.notification;

import org.mule.api.MuleMessage;

/**
 * Signals that a Notification can have a {@link MuleMessage} available through it.  The message set will be the one
 * available when the notification was triggered.
 * Note that the message returned may also be a {@link org.mule.api.MuleMessageCollection} if there was more than one
 * message available when the notification was created.
 *
 * @see org.mule.api.MuleMessage
 * @see org.mule.api.MuleMessageCollection
 */
public interface MuleMessageAware
{
    public MuleMessage getMessage();
}
