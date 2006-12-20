/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.mule.config.ExceptionReader;

/**
 * Surfaces information about SQLExceptions such as the code and sql state. Also uses
 * the NextException to find the cause
 */
public class SQLExceptionReader implements ExceptionReader
{
    public String getMessage(Throwable t)
    {
        SQLException e = (SQLException)t;
        return e.getMessage() + "(SQL Code: " + e.getErrorCode() + ", SQL State: + " + e.getSQLState() + ")";
    }

    public Throwable getCause(Throwable t)
    {
        SQLException e = (SQLException)t;
        Throwable cause = e.getNextException();
        if (cause == null)
        {
            cause = e.getCause();
        }
        return cause;
    }

    public Class getExceptionType()
    {
        return SQLException.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @param t the exception to extract the information from
     * @return a map of the non-stanard information stored on the exception
     */
    public Map getInfo(Throwable t)
    {
        SQLException e = (SQLException)t;
        Map info = new HashMap();
        if (e.getErrorCode() != 0)
        {
            info.put("SQL Code", String.valueOf(e.getErrorCode()));
        }
        if (e.getSQLState() != null && !e.getSQLState().equals(StringUtils.EMPTY))
        {
            info.put("SQL State", e.getSQLState());
        }
        return info;
    }
}
