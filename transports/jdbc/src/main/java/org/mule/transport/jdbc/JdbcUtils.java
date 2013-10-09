/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Utility methods for working with various parts of JDBC.
 */
public final class JdbcUtils
{

    private JdbcUtils()
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
