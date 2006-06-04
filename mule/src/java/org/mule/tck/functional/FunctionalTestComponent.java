/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *
 */

package org.mule.tck.functional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.util.StringMessageUtils;

/**
 * <code>FunctionalTestComponent</code> is a component that can be used by
 * functional tests. This component accepts an EventCallback that can be used to
 * assert the state of the current event.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 * @see EventCallback
 */

public class FunctionalTestComponent implements Callable
{
    private static transient Log logger = LogFactory.getLog(FunctionalTestComponent.class);

    private EventCallback eventCallback;
    private String returnMessage = null;
    private boolean throwException = false;

    public Object onCall(UMOEventContext context) throws Exception
    {
        String contents = context.getTransformedMessageAsString();
        String msg = null;
        msg = StringMessageUtils.getBoilerPlate("Message Received in component: "
                + context.getComponentDescriptor().getName() + ". Content is: "
                + StringMessageUtils.truncate(contents, 100, true), '*', 80);
        logger.info(msg);


        if (eventCallback != null) {
            eventCallback.eventReceived(context, this);
        }
        String replyMessage;
        if(returnMessage!=null) {
            replyMessage = returnMessage;
        } else {
            replyMessage = contents + " Received";
        }

        MuleManager.getInstance().fireNotification(new FunctionalTestNotification(context, replyMessage, FunctionalTestNotification.EVENT_RECEIVED));

        if(throwException) {
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

    public String getReturnMessage() {
        return returnMessage;
    }

    public void setReturnMessage(String returnMessage) {
        this.returnMessage = returnMessage;
    }

    public boolean isThrowException() {
        return throwException;
    }

    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }
}
