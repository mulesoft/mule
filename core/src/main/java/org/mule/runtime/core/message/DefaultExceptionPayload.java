/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
