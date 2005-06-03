/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

/**
 * <code>ComponentException</code> should be thrown when some action on a
 * component fails such as starting or stopping
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ComponentException extends MessagingException
{
    private transient UMOComponent component;

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

    private static Message generateMessage(Message message, UMOComponent component)
    {
        Message m = new Message(Messages.COMPONENT_CAUSED_ERROR_IS_X, component);
        if (message != null) {
            message.setNextMessage(m);
            return message;
        } else {
            message = new Message(0);
            return m;
        }
    }
}
