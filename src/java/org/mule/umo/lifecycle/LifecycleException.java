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
package org.mule.umo.lifecycle;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOException;

/**
 * <code>LifecycleException</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class LifecycleException extends UMOException
{
    private transient Object component;

    /**
     * @param message   the exception message
     * @param component the object that failed during a lifecycle method call
     */
    public LifecycleException(Message message, Object component) {
        super(message);
        this.component = component;
    }

    /**
     * @param message   the exception message
     * @param cause     the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public LifecycleException(Message message, Throwable cause, Object component) {
        super(message, cause);
        this.component = component;
    }

    /**
     * @param cause     the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public LifecycleException(Throwable cause, Object component) {
        super(new Message(Messages.INITIALISATION_FAILURE_X, cause.getMessage()), cause);
        this.component = component;
        addDetails();
    }


    private void addDetails() {
        Message m = new Message(Messages.OBJECT_CAUSED_ERROR_IS_X, component);
        addInfo("Object", component.toString());
    }

    public Object getComponent() {
        return component;
    }
}
