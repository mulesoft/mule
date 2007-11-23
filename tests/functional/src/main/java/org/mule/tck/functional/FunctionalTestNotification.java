/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.functional;

import org.mule.impl.internal.notifications.CustomNotification;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

/**
 * A <code>FunctionlTestNotification</code> is fired by the {@link org.mule.tck.functional.FunctionalTestComponent}
 * when it receives an event. Test cases can register a {@link org.mule.tck.functional.FunctionalTestNotificationListener}
 * with Mule to receive these notifications and make assertions about the number of messages received or the content
 * of the message.
 * <p/>
 * This Notification contains the current EventContext and reply message. The resource Identifier for this event
 * is the component name that received the message.  This means you can register to listen to Notifications from a
 * selected {@link org.mule.tck.functional.FunctionalTestComponent}. i.e.
 * <code>
 * managementContext.registerListener(this, "*JmsTestCompoennt");
 * </code>
 * <p/>
 * This registration would only receive {@link org.mule.tck.functional.FunctionalTestNotification} objects
 * from components called 'MyJmsTestComponent' and 'YourJmsTestComponent' but not 'HerFileTestComponent'.
 *
 * @see org.mule.tck.functional.FunctionalTestComponent
 * @see org.mule.tck.functional.FunctionalTestNotificationListener
 * @see org.mule.umo.UMOManagementContext
 */
public class FunctionalTestNotification extends CustomNotification
{
    /** Serial version */
    private static final long serialVersionUID = -3435373745940904597L;

    public static final int EVENT_RECEIVED = -999999;

    static
    {
        registerAction("event received", EVENT_RECEIVED);
    }

    private final Object replyMessage;
    private final UMOEventContext eventContext;

    public FunctionalTestNotification(UMOEventContext context, Object replyMessage, int action)
            throws TransformerException
    {
        super(context.getTransformedMessage(), action);
        resourceIdentifier = context.getComponent().getName();
        this.replyMessage = replyMessage;
        this.eventContext = context;
    }

    public Object getReplyMessage()
    {
        return replyMessage;
    }

    public UMOEventContext getEventContext()
    {
        return eventContext;
    }
}
