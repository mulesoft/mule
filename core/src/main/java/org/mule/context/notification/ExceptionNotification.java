/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
