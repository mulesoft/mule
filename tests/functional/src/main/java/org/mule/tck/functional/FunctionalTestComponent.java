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

import org.mule.MuleException;
import org.mule.config.i18n.Message;
import org.mule.impl.RequestContext;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.util.StringMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>FunctionalTestComponent</code> is a component that can be used by
 * functional tests. This component accepts an EventCallback that can be used to
 * assert the state of the current event.
 * 
 * @see EventCallback
 */

public class FunctionalTestComponent implements Callable
{
    protected transient Log logger = LogFactory.getLog(getClass());

    private EventCallback eventCallback;
    private Object returnMessage = null;
    private boolean throwException = false;

    public Object onCall(UMOEventContext context) throws Exception
    {
        String contents = context.getTransformedMessageAsString();
        String msg = StringMessageUtils.getBoilerPlate("Message Received in component: "
                        + context.getComponentDescriptor().getName() + ". Content is: "
                        + StringMessageUtils.truncate(contents, 100, true), '*', 80);

        logger.info(msg);

        if (eventCallback != null)
        {
            eventCallback.eventReceived(context, this);
        }

        Object replyMessage;
        if (returnMessage != null)
        {
            replyMessage = returnMessage;
        }
        else
        {
            replyMessage = contents + " Received";
        }

        context.getManagmentContext().fireNotification(
            new FunctionalTestNotification(context, replyMessage, FunctionalTestNotification.EVENT_RECEIVED));

        if (throwException)
        {
            throw new MuleException(Message.createStaticMessage("Functional Test Component Exception"));
        }

        return replyMessage;
    }

    public Object onReceive(Object data) throws Exception
    {
        UMOEventContext context = RequestContext.getEventContext();

        String contents = data.toString();
        String msg = StringMessageUtils.getBoilerPlate("Message Received in component: "
                        + context.getComponentDescriptor().getName() + ". Content is: "
                        + StringMessageUtils.truncate(contents, 100, true), '*', 80);

        logger.info(msg);

        if (eventCallback != null)
        {
            eventCallback.eventReceived(context, this);
        }

        Object replyMessage;
        if (returnMessage != null)
        {
            replyMessage = returnMessage;
        }
        else
        {
            replyMessage = contents + " Received";
        }

        context.getManagmentContext().fireNotification(
            new FunctionalTestNotification(context, replyMessage, FunctionalTestNotification.EVENT_RECEIVED));

        if (throwException)
        {
            throw new MuleException(Message.createStaticMessage("Functional Test Component Exception"));
        }

        return replyMessage;
    }

    public EventCallback getEventCallback()
    {
        return eventCallback;
    }

    public void setEventCallback(EventCallback eventCallback)
    {
        this.eventCallback = eventCallback;
    }

    public Object getReturnMessage()
    {
        return returnMessage;
    }

    public void setReturnMessage(Object returnMessage)
    {
        this.returnMessage = returnMessage;
    }

    public boolean isThrowException()
    {
        return throwException;
    }

    public void setThrowException(boolean throwException)
    {
        this.throwException = throwException;
    }

}
