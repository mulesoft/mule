/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

/**
 * <code>ComponentException</code> should be thrown when some action on a component
 * fails, such as starting or stopping
 */
// @Immutable
public class ComponentException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 56178344205041599L;

    private transient final UMOComponent component;

    /**
     * @param message the exception message
     */
    public ComponentException(Message message, UMOMessage umoMessage, UMOComponent component)
    {
        super(generateMessage(message, component), umoMessage);
        this.component = component;
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public ComponentException(Message message, UMOMessage umoMessage, UMOComponent component, Throwable cause)
    {
        super(generateMessage(message, component), umoMessage, cause);
        this.component = component;
    }

    public ComponentException(UMOMessage umoMessage, UMOComponent component, Throwable cause)
    {
        super(generateMessage(null, component), umoMessage, cause);
        this.component = component;
    }

    public UMOComponent getComponent()
    {
        return component;
    }

    private static Message generateMessage(Message previousMessage, UMOComponent component)
    {
        Message returnMessage = new Message(Messages.COMPONENT_CAUSED_ERROR_IS_X, component);
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
