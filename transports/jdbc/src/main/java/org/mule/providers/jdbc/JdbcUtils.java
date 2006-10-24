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

import org.mule.util.properties.PropertyExtractor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for working with various parts of JDBC.
 */
public abstract class JdbcUtils
{

    protected static Set extractors = new HashSet();

    public static void addPropertyExtractor(PropertyExtractor extractor)
    {
        extractors.add(extractor);
    }

    public static void removePropertyExtractor(PropertyExtractor extractor)
    {
        extractors.remove(extractor);
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
            if (con.getAutoCommit() == false)
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
            if (con.getAutoCommit() == false)
            {
                con.rollback();
            }
            con.close();
        }
    }

}
