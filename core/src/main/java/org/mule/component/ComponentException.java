/*
 * $Id: ComponentException.java 14179 2009-02-27 18:46:19Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.component.Component;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

/**
 * <code>ComponentException</code> should be thrown when some action on a component
 * fails, such as starting or stopping
 */
// @ThreadSafe
public class ComponentException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 56178344205041600L;

    private final transient Component component;

    /**
     * @param message the exception message
     * @deprecated use ComponentException(Message, MuleEvent, Component)
     */
    @Deprecated
    public ComponentException(Message message, MuleMessage muleMessage, Component component)
    {
        super(generateMessage(message, component), muleMessage);
        this.component = component;
    }

    public ComponentException(Message message, MuleEvent muleMessage, Component component)
    {
        super(generateMessage(message, component), muleMessage);
        this.component = component;
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     * @deprecated use ComponentException(Message, MuleEvent, Component, Throwable)
     */
    @Deprecated
    public ComponentException(Message message, MuleMessage muleMessage, Component component, Throwable cause)
    {
        super(generateMessage(message, component), muleMessage, cause);
        this.component = component;
    }

    public ComponentException(Message message, MuleEvent event, Component component, Throwable cause)
    {
        super(generateMessage(message, component), event, cause);
        this.component = component;
    }

    /**
     * @deprecated use ComponentException(MuleEvent, Component, Throwable)
     */
    @Deprecated
    public ComponentException(MuleMessage message, Component component, Throwable cause)
    {
        super(generateMessage(null, component), message, cause);
        this.component = component;
    }

    public ComponentException(MuleEvent message, Component component, Throwable cause)
    {
        super(generateMessage(null, component), message, cause);
        this.component = component;
    }

    public Component getComponent()
    {
        return component;
    }

    private static Message generateMessage(Message previousMessage, Component component)
    {
        Message returnMessage = CoreMessages.componentCausedErrorIs(component);
        if (previousMessage != null)
        {
            previousMessage.setNextMessage(returnMessage);
            return previousMessage;
        }
        else
        {
            return returnMessage;
        }
    }
}
