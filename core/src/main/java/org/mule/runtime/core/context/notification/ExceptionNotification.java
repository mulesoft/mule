/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.mule.api.context.notification.ServerNotification;

/**
 * This class is from Mule 2.2.5. It is modified so the ExceptionNotification has a
 * resourceId of the exception type. This is only here so we can avoid doing a hot
 * fix of Mule to run MMC. This will be removed in future releases of MMC.
 */
public class ExceptionNotification extends ServerNotification
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
        super(exception, EXCEPTION_ACTION, getExceptionCause(exception));
        this.exception = exception;
    }

    /**
     * Find the root cause of the exception as typically Mule wraps the exception in
     * something like a ServiceException and when we register a listener under a
     * particular resource ID we want to listen for this root cause, not the
     * ServiceException.
     * 
     * @param exception
     * @return
     */
    private static String getExceptionCause(Throwable exception)
    {
        Throwable cause = ExceptionUtils.getRootCause(exception);
        if (cause != null)
        {
            return cause.getClass().getName();
        }
        else
        {
            return null;
        }
    }

    public Throwable getException()
    {
        return this.exception;
    }

    @Override
    public String getType()
    {
        return TYPE_ERROR;
    }
}
