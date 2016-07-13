/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.functional;

import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.context.notification.CustomNotification;

/**
 * A <code>FunctionlTestNotification</code> is fired by the {@link FunctionalTestComponent}
 * when it receives an event. Test cases can register a {@link FunctionalTestNotificationListener}
 * with Mule to receive these notifications and make assertions about the number of messages received or the content
 * of the message.
 * <p/>
 * This Notification contains the current MuleEventContext and reply message. The resource Identifier for this event
 * is the service name that received the message.  This means you can register to listen to Notifications from a
 * selected {@link FunctionalTestComponent}. i.e.
 * <code>
 * muleContext.registerListener(this, "*JmsTestCompoennt");
 * </code>
 * <p/>
 * This registration would only receive {@link FunctionalTestNotification} objects
 * from components called 'MyJmsTestComponent' and 'YourJmsTestComponent' but not 'HerFileTestComponent'.
 *
 * @see FunctionalTestComponent
 * @see FunctionalTestNotificationListener
 * @see org.mule.runtime.core.api.MuleContext
 */
public class FunctionalTestNotification extends CustomNotification
{
    /** Serial version */
    private static final long serialVersionUID = -3435373745940904597L;

    public static final int EVENT_RECEIVED = -999999;

    static
    {
        //TODO(pablo.kraan): find a better way to manage this situation.
        try
        {
            registerAction("event received", EVENT_RECEIVED);
        }
        catch (IllegalStateException e)
        {
            // Ignore
        }
    }

    private final Object replyMessage;
    private final MuleEventContext eventContext;

    public FunctionalTestNotification(Object message, int action)
    {
        super(message, action);
        this.replyMessage = null;
        this.eventContext = RequestContext.getEventContext();
        resourceIdentifier = eventContext.getFlowConstruct().getName();

    }

    public FunctionalTestNotification(MuleEventContext context, Object replyMessage, int action)
            throws TransformerException
    {
        super(context.getMessage().getPayload(), action);
        resourceIdentifier = context.getFlowConstruct().getName();
        this.replyMessage = replyMessage;
        this.eventContext = context;
    }

    public Object getReplyMessage()
    {
        return replyMessage;
    }

    public MuleEventContext getEventContext()
    {
        return eventContext;
    }
}
