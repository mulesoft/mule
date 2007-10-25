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

import org.mule.MuleException;
import org.mule.MuleServer;
import org.mule.config.i18n.MessageFactory;
import org.mule.impl.RequestContext;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.util.NumberUtils;
import org.mule.util.StringMessageUtils;

import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>FunctionalTestComponent</code> is a component that can be used by
 * functional tests. This component accepts an EventCallback that can be used to
 * assert the state of the current event.
 * <p/>
 * Also, this component fires {@link FunctionalTestNotification} via Mule for every message received.
 * Tests can register with Mule to receive these events by implementing
 * {@link FunctionalTestNotificationListener}.
 *
 * @see org.mule.tck.functional.EventCallback
 * @see org.mule.tck.functional.FunctionalTestNotification
 * @see org.mule.tck.functional.FunctionalTestNotificationListener
 */

public class FunctionalTestComponent implements Callable, Initialisable, Disposable
{
    protected transient Log logger = LogFactory.getLog(getClass());

    public static final int STREAM_SAMPLE_SIZE = 4;
    public static final int STREAM_BUFFER_SIZE = 4096;
    private EventCallback eventCallback;
    private Object returnMessage = null;
    private boolean appendComponentName = false;
    private boolean throwException = false;
    private boolean enableMessageHistory = true;
    private boolean addReceived = true;

    /**
     * Keeps a list of any messages received on this component. Note that only references
     * to the messages (objects) are stored, so any subsequent changes to the objects
     * will change the history.
     */
    private List messageHistory;

    public void initialise()
    {
        if (enableMessageHistory)
        {
            messageHistory = new CopyOnWriteArrayList();
        }
    }

    public void dispose()
    {
        // nothing to do
    }

    /** {@inheritDoc} */
    public Object onCall(UMOEventContext context) throws Exception
    {
        if (enableMessageHistory)
        {
            messageHistory.add(context.getTransformedMessage());
        }

        String contents = context.getTransformedMessageAsString();
        String msg = StringMessageUtils.getBoilerPlate("Message Received in component: "
                + context.getComponent().getName() + ". Content is: "
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
            replyMessage = (addReceived ? received(contents) : contents)
                    + (appendComponentName ? " " + context.getComponent().getName() : "");
        }

        UMOManagementContext managementContext = context.getManagementContext();
        if (managementContext == null)
        {
            logger.warn("No ManagementContext available from EventContext");
            managementContext = MuleServer.getManagementContext();
        }
        managementContext.fireNotification(
            new FunctionalTestNotification(context, replyMessage, FunctionalTestNotification.EVENT_RECEIVED));

        if (throwException)
        {
            throw new MuleException(MessageFactory.createStaticMessage("Functional Test Component Exception"));
        }

        return replyMessage;
    }

    /**
     * Append " Received" to contents.  Exposed as static method so tests can call to
     * construct string for comparison.
     *
     * @param contents
     * @return Extended message
     */
    public static String received(String contents)
    {
        return contents + " Received";
    }

    /**
     * This method duplicates much of the functionality for the {@link #onCall} method above. This method is currently
     * used by some WebServices tests where you don' want to be introducing the {@link org.mule.umo.UMOEventContext} as
     * a complex type.
     * TODO: It would be nice to remove this method or at least refactor the methods so there is little or no duplication
     *
     * @param data the event data received
     * @return the processed message
     * @throws Exception
     */
    public Object onReceive(Object data) throws Exception
    {
        UMOEventContext context = RequestContext.getEventContext();
        String contents = data.toString();
        String msg = StringMessageUtils.getBoilerPlate("Message Received in component: "
                + context.getComponent().getName() + ". Content is: "
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

        context.getManagementContext().fireNotification(
                new FunctionalTestNotification(context, replyMessage, FunctionalTestNotification.EVENT_RECEIVED));

        if (throwException)
        {
            if (returnMessage != null && returnMessage instanceof Exception)
            {
                throw (Exception) returnMessage;
            }
            else
            {
                throw new MuleException(MessageFactory.createStaticMessage("Functional Test Component Exception"));
            }
        }

        return replyMessage;
    }

    /**
     * An event callback is called when a message is received by the component.
     * An Event callback isn't strictly required but it is usfal for performing assertions
     * on the current message being received.
     * Note that the FunctionalTestComponent should be made a singleton
     * {@link org.mule.umo.UMODescriptor#setSingleton} when using Event callbacks
     * <p/>
     * Another option is to register a {@link FunctionalTestNotificationListener} with Mule and this
     * will deleiver a {@link FunctionalTestNotification} for every message received by this component
     *
     * @return the callback to call when a message is received
     * @see org.mule.umo.UMODescriptor
     * @see org.mule.tck.functional.FunctionalTestNotification
     * @see org.mule.tck.functional.FunctionalTestNotificationListener
     */
    public EventCallback getEventCallback()
    {
        return eventCallback;
    }

    /**
     * An event callback is called when a message is received by the component.
     * An Event callback isn't strictly required but it is usfal for performing assertions
     * on the current message being received.
     * Note that the FunctionalTestComponent should be made a singleton
     * {@link org.mule.umo.UMODescriptor#setSingleton} when using Event callbacks
     * <p/>
     * Another option is to register a {@link FunctionalTestNotificationListener} with Mule and this
     * will deleiver a {@link FunctionalTestNotification} for every message received by this component
     *
     * @param eventCallback the callback to call when a message is received
     * @see org.mule.umo.UMODescriptor
     * @see org.mule.tck.functional.FunctionalTestNotification
     * @see org.mule.tck.functional.FunctionalTestNotificationListener
     */
    public void setEventCallback(EventCallback eventCallback)
    {
        this.eventCallback = eventCallback;
    }

    /**
     * Often you will may want to return a fixed message payload to simulate and external system call.
     * This can be done using the 'returnMessage' property. Note that you can return complex objects by
     * using the <container-property> element in the Xml configuration.
     *
     * @return the message payload to always return from this component instance
     */
    public Object getReturnMessage()
    {
        return returnMessage;
    }

    /**
     * Often you will may want to return a fixed message payload to simulate and external system call.
     * This can be done using the 'returnMessage' property. Note that you can return complex objects by
     * using the <container-property> element in the Xml configuration.
     *
     * @param returnMessage the message payload to always return from this component instance
     */
    public void setReturnMessage(Object returnMessage)
    {
        this.returnMessage = returnMessage;
    }

    /**
     * Sometimes you will want the component to always throw an exception, if this is the case you can
     * set the 'throwException' property to true.
     *
     * @return throwException true if an exception should always be thrown from this instance.
     *         If the {@link #returnMessage} property is set and is of type
     *         java.lang.Exception, that exception will be thrown.
     */
    public boolean isThrowException()
    {
        return throwException;
    }

    /**
     * Sometimes you will want the component to always throw an exception, if this is the case you can
     * set the 'throwException' property to true.
     *
     * @param throwException true if an exception should always be thrown from this instance.
     *                       If the {@link #returnMessage} property is set and is of type
     *                       java.lang.Exception, that exception will be thrown.
     */
    public void setThrowException(boolean throwException)
    {
        this.throwException = throwException;
    }

    /**
     * This will cause the component to append the compoent name to the end of the message
     * returned from this component. This only works when processing String messages.
     * This feature is useful when processing multiple messages using a pool of FunctionalTestComponents
     * to determine who processed the resulting message
     *
     * @return true if the component name will be appended to the return message
     */
    public boolean isAppendComponentName()
    {
        return appendComponentName;
    }

    /**
     * This will cause the component to append the compoent name to the end of the message
     * returned from this component. This only works when processing String messages.
     * This feature is useful when processing multiple messages using a pool of FunctionalTestComponents
     * to determine who processed the resulting message
     *
     * @param appendComponentName true if the component name will be appended to the return message
     */
    public void setAppendComponentName(boolean appendComponentName)
    {
        this.appendComponentName = appendComponentName;
    }

    public boolean isEnableMessageHistory()
    {
        return enableMessageHistory;
    }

    public void setEnableMessageHistory(boolean enableMessageHistory)
    {
        this.enableMessageHistory = enableMessageHistory;
    }

    /** If enableMessageHistory = true, returns the number of messages received by this component. */
    public int getReceivedMessages()
    {
        if (messageHistory != null)
        {
            return messageHistory.size();
        }
        else
        {
            return NumberUtils.INTEGER_MINUS_ONE.intValue();
        }
    }

    /**
     * If enableMessageHistory = true, returns a message received by the component in chronological order.
     * For example, getReceivedMessage(1) returns the first message received by the component,
     * getReceivedMessage(2) returns the second message received by the component, etc.
     */
    public Object getReceivedMessage(int number)
    {
        Object message = null;
        if (messageHistory != null)
        {
            if (number <= messageHistory.size())
            {
                message = messageHistory.get(number - 1);
            }
        }
        return message;
    }

    /** If enableMessageHistory = true, returns the last message received by the component in chronological order. */
    public Object getLastReceivedMessage()
    {
        if (messageHistory != null)
        {
            return messageHistory.get(messageHistory.size() - 1);
        }
        else
        {
            return null;
        }
    }

    public boolean isAddReceived()
    {
        return addReceived;
    }

    public void setAddReceived(boolean addReceived)
    {
        this.addReceived = addReceived;
    }
}

