/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.message;

import org.mule.api.MuleException;
import org.mule.api.ExceptionPayload;
import org.mule.config.ExceptionHelper;

import java.util.Map;

/**
 * <code>DefaultExceptionPayload</code> TODO
 */

public class DefaultExceptionPayload implements ExceptionPayload
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7114836033686599024L;

    private int code = 0;
    private String message = null;
    private Map info = null;
    private Throwable exception;

    public DefaultExceptionPayload(Throwable exception)
    {
        this.exception = exception;
        MuleException muleRoot = ExceptionHelper.getRootMuleException(exception);
        if (muleRoot != null)
        {
            message = muleRoot.getMessage();
            code = muleRoot.getExceptionCode();
            info = muleRoot.getInfo();
        }
        else
        {
            message = exception.getMessage();
        }
    }

    public Throwable getRootException()
    {
        return ExceptionHelper.getRootException(exception);
    }

    public int getCode()
    {
        return code;
    }

    public String getMessage()
    {
        return message;
    }

    public Map getInfo()
    {
        return info;
    }

    public Throwable getException()
    {
        return exception;
    }

}
