/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.lifecycle;

import org.mule.config.i18n.Message;

/**
 * <code>CreateException</code> is thrown when creating an object inside Mule wasn't possible due
 * to inconsistent internal state or wrong input.
 */
public class CreateException extends LifecycleException
{
    /** Serial version */
    private static final long serialVersionUID = -8402348927606781921L;

    /**
     * @param message   the exception message
     * @param service the object that failed during a lifecycle method call
     */
    public CreateException(Message message, Object component)
    {
        super(message, component);
    }

    /**
     * @param message   the exception message
     * @param cause     the exception that cause this exception to be thrown
     * @param service the object that failed during a lifecycle method call
     */
    public CreateException(Message message, Throwable cause, Object component)
    {
        super(message, cause, component);
    }

    /**
     * @param cause     the exception that cause this exception to be thrown
     * @param service the object that failed during a lifecycle method call
     */
    public CreateException(Throwable cause, Object component)
    {
        super(cause, component);
    }
}