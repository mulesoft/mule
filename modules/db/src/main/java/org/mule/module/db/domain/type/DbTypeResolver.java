/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.domain.type;

import org.mule.module.db.domain.connection.DbConnection;
import org.mule.module.db.domain.query.QueryTemplate;

import java.sql.SQLException;
import java.util.Map;

/**
 * Resolves parameter types on a given database instance
 */
public interface DbTypeResolver
{

    /**
     * Resolves types for a query template's parameters
     *
     * @param connection connection uses to execute the query
     * @param queryTemplate query template containing parameters
     * @return a map containing param index/param type pairs
     * @throws SQLException if a database access error occurs or this method is called on a closed connection
     */
    Map<Integer, DbType> getParameterTypes(DbConnection connection, QueryTemplate queryTemplate) throws SQLException;
}
