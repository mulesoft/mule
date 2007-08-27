/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Utility methods for working with various parts of JDBC.
 */
public abstract class JdbcUtils
{

    protected JdbcUtils()
    {
        // empty, just to restrict instanciation
    }

    public static void close(Connection con) throws SQLException
    {
        if (con != null && !con.isClosed())
        {
            con.close();
        }
    }

    public static void commitAndClose(Connection con) throws SQLException
    {
        if (con != null)
        {
            if (!con.getAutoCommit())
            {
                con.commit();
            }
            con.close();
        }
    }

    public static void rollbackAndClose(Connection con) throws SQLException
    {
        if (con != null)
        {
            if (!con.getAutoCommit())
            {
                con.rollback();
            }
            con.close();
        }
    }

}
