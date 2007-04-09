/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.mule.umo.UMOException;

import java.util.Collections;
import java.util.Map;

/**
 * Grabs all information from the UMOException type
 */
public final class UMOExceptionReader implements ExceptionReader
{

    public String getMessage(Throwable t)
    {
        return t.getMessage();
    }

    public Throwable getCause(Throwable t)
    {
        return t.getCause();
    }

    public Class getExceptionType()
    {
        return UMOException.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @return a map of the non-stanard information stored on the exception
     */
    public Map getInfo(Throwable t)
    {
        return (t instanceof UMOException ? ((UMOException) t).getInfo() : Collections.EMPTY_MAP);
    }

}
