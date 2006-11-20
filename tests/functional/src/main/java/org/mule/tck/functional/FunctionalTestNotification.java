/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.functional;

import org.mule.impl.internal.notifications.CustomNotification;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

/**
 * TODO document
 */
public class FunctionalTestNotification extends CustomNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3435373745940904597L;

    public static final int EVENT_RECEIVED = -999999;

    private final Object replyMessage;
    private final UMOEventContext eventContext;

    public FunctionalTestNotification(UMOEventContext context, Object replyMessage, int action)
        throws TransformerException
    {
        super(context.getTransformedMessage(), action);
        resourceIdentifier = context.getComponentDescriptor().getName();
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
