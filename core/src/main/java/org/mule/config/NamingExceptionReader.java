/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingException;

public class NamingExceptionReader implements ExceptionReader
{
    /**
     * Displayed when no remaining or resolved name found.
     */
    protected static final String MISSING_NAME_DISPLAY_VALUE = "<none>";

    public String getMessage(Throwable t)
    {
        return (t instanceof NamingException ? ((NamingException) t).toString(true) : "<unknown>");
    }

    public Throwable getCause(Throwable t)
    {
        return (t instanceof NamingException ? ((NamingException) t).getCause() : null);
    }

    public Class getExceptionType()
    {
        return NamingException.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @param t the exception to extract the information from
     * @return a map of the non-stanard information stored on the exception
     */
    public Map getInfo(Throwable t)
    {
        if (t instanceof NamingException)
        {
            NamingException e = (NamingException) t;
            Map info = new HashMap();
            final Name remainingName = e.getRemainingName();
            final Name resolvedName = e.getResolvedName();
            info.put("Remaining Name", remainingName == null
                            ? MISSING_NAME_DISPLAY_VALUE : remainingName.toString());
            info.put("Resolved Name", resolvedName == null ? MISSING_NAME_DISPLAY_VALUE : resolvedName.toString());
            return info;
        }
        else
        {
            return Collections.EMPTY_MAP;
        }
    }
}
