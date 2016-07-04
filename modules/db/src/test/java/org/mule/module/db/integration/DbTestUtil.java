/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration;

import static org.junit.Assert.assertTrue;

import com.mysql.jdbc.Statement;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;

public class DbTestUtil
{

    public static <T> List<Map<String, T>> selectData(String query, DataSource dataSource) throws SQLException
    {
        QueryRunner qr = new QueryRunner(dataSource);

        @SuppressWarnings({"unchecked"})
        List<Map<String, T>> result = (List<Map<String, T>>) qr.query(query, new MapListHandler());

        return result;
    }

    public static void assertExpectedUpdateCount(int expected, int actual)
    {
        assertTrue(String.format("Update count is neither the expected one %s nor Statement.SUCCESS_NO_INFO", expected),
                   expected == actual || Statement.SUCCESS_NO_INFO == actual);
    }
}
