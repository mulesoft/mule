/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config;

import org.mule.api.config.ExceptionReader;

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

    public Class<?> getExceptionType()
    {
        return NamingException.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @param t the exception to extract the information from
     * @return a map of the non-stanard information stored on the exception
     */
    public Map<?, ?> getInfo(Throwable t)
    {
        if (t instanceof NamingException)
        {
            NamingException e = (NamingException) t;
            
            Map<String, Object> info = new HashMap<String, Object>();
            final Name remainingName = e.getRemainingName();
            final Name resolvedName = e.getResolvedName();
            info.put("Remaining Name", remainingName == null ?
                    MISSING_NAME_DISPLAY_VALUE : remainingName.toString());
            info.put("Resolved Name", resolvedName == null ? 
                MISSING_NAME_DISPLAY_VALUE : resolvedName.toString());
            return info;
        }
        else
        {
            return Collections.EMPTY_MAP;
        }
    }
}
