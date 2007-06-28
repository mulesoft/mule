/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications;

import org.mule.umo.manager.UMOServerNotification;

public class ExceptionNotification extends UMOServerNotification
{
    /**
     * Serial version.
     */
    private static final long serialVersionUID = -43091546451476239L;
    public static final int EXCEPTION_ACTION = EXCEPTION_EVENT_ACTION_START_RANGE + 1;

    static
    {
        registerAction("exception", EXCEPTION_ACTION);
    }

    private Throwable exception;

    public ExceptionNotification(Throwable exception)
    {
        super(exception, EXCEPTION_ACTION);
        this.exception = exception;
    }

    public Throwable getException()
    {
        return this.exception;
    }

    public String getType()
    {
        return TYPE_ERROR;
    }
}
