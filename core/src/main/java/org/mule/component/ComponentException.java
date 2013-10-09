/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.component;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
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

    public ComponentException(Message message, MuleEvent muleMessage, Component component)
    {
        super(generateMessage(message, component), muleMessage);
        this.component = component;
    }

    public ComponentException(Message message, MuleEvent event, Component component, Throwable cause)
    {
        super(generateMessage(message, component), event, cause);
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
