/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.message;

import org.mule.config.ExceptionHelper;
import org.mule.umo.UMOException;
import org.mule.umo.UMOExceptionPayload;

import java.util.Map;

/**
 * <code>ExceptionPayload</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ExceptionPayload implements UMOExceptionPayload
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7114836033686599024L;

    private int code = 0;
    private String message = null;
    private Map info = null;
    private Throwable exception;

    public ExceptionPayload(Throwable exception)
    {
        this.exception = exception;
        UMOException muleRoot = ExceptionHelper.getRootMuleException(exception);
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
