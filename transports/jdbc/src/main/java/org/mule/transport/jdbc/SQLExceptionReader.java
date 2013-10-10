/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc;

import org.mule.api.config.ExceptionReader;
import org.mule.util.StringUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Surfaces information about SQLExceptions such as the code and sql state. Also uses
 * the NextException to find the cause
 */
public class SQLExceptionReader implements ExceptionReader
{
    public String getMessage(Throwable t)
    {
        SQLException e = (SQLException) t;
        return e.getMessage() + "(SQL Code: " + e.getErrorCode() + ", SQL State: + " + e.getSQLState() + ")";
    }

    public Throwable getCause(Throwable t)
    {
        SQLException e = (SQLException) t;
        Throwable cause = e.getNextException();
        if (cause == null)
        {
            cause = e.getCause();
        }
        return cause;
    }

    public Class<?> getExceptionType()
    {
        return SQLException.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @param t the exception to extract the information from
     * @return a map of the non-stanard information stored on the exception
     */
    public Map<?, ?> getInfo(Throwable t)
    {
        SQLException e = (SQLException) t;
        
        Map<String, Object> info = new HashMap<String, Object>();
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
