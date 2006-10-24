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

import javax.naming.Name;
import javax.naming.NamingException;

import java.util.HashMap;
import java.util.Map;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class NamingExceptionReader implements ExceptionReader
{

    /**
     * Displayed when no remaining or resolved name found.
     */
    protected static final String MISSING_NAME_DISPLAY_VALUE = "<none>";

    public String getMessage(Throwable t)
    {
        NamingException e = (NamingException)t;
        return e.toString(true);
    }

    public Throwable getCause(Throwable t)
    {
        NamingException e = (NamingException)t;
        return e.getRootCause();
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
        NamingException e = (NamingException)t;
        Map info = new HashMap();
        final Name remainingName = e.getRemainingName();
        final Name resolvedName = e.getResolvedName();
        info.put("Remaining Name", remainingName == null
                        ? MISSING_NAME_DISPLAY_VALUE : remainingName.toString());
        info.put("Resolved Name", resolvedName == null ? MISSING_NAME_DISPLAY_VALUE : resolvedName.toString());
        return info;
    }
}
