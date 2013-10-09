/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
