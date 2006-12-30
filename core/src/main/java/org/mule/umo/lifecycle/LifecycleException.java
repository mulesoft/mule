/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.lifecycle;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOException;

import org.apache.commons.lang.ObjectUtils;

/**
 * <code>LifecycleException</code> TODO
 */

public class LifecycleException extends UMOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 2909614055858287394L;

    private transient Object component;

    /**
     * @param message the exception message
     * @param component the object that failed during a lifecycle method call
     */
    public LifecycleException(Message message, Object component)
    {
        super(message);
        this.component = component;
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public LifecycleException(Message message, Throwable cause, Object component)
    {
        super(message, cause);
        this.component = component;
    }

    /**
     * @param cause the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public LifecycleException(Throwable cause, Object component)
    {
        super(new Message(Messages.INITIALISATION_FAILURE_X, cause.getMessage()), cause);
        this.component = component;
        addInfo("Object", ObjectUtils.toString(component, "null"));
    }

    public Object getComponent()
    {
        return component;
    }

}
