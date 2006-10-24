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

import java.util.HashMap;
import java.util.Map;

/**
 * This is the default exception reader used if there is no specific one registered
 * for the current exception.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public final class DefaultExceptionReader implements ExceptionReader
{

    private Map info = new HashMap();

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
        return Throwable.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @return a map of the non-stanard information stored on the exception
     */
    public Map getInfo(Throwable t)
    {
        return info;
    }
}
