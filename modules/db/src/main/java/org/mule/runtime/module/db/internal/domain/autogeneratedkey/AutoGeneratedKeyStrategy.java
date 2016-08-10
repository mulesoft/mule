/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.internal.domain.autogeneratedkey;

import org.mule.runtime.module.db.internal.domain.connection.DbConnection;
import org.mule.runtime.module.db.internal.domain.query.QueryTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages different ways to process auto generated keys
 */
public interface AutoGeneratedKeyStrategy
{

    /**
     * Indicates if there are auto generated keys to return
     * @return true if there are auto generated keys, false otherwise
     */
    boolean returnsAutoGeneratedKeys();

    /**
     * Prepares a statement in order to return auto generated keys
     *
     * @param connection connection uses to prepare the statement
     * @param queryTemplate query template to be prepared
     * @return a statement prepared to return auto generated keys
     * @throws SQLException if there is any error preparing the query
     */
    PreparedStatement prepareStatement(DbConnection connection, QueryTemplate queryTemplate) throws SQLException;

    /**
     * Executes a query that can return multiple results
     *
     * @param statement statement to be executed
     * @param queryTemplate query to execute
     * @return true if the first result is a ResultSet object; false if it is an update count or there are no results
     * @throws SQLException if there is any database error or this method is called on a closed Statement
     */
    boolean execute(Statement statement, QueryTemplate queryTemplate) throws SQLException;

    /**
     * Executes an update query
     *
     * @param statement statement to be executed
     * @param queryTemplate query to execute
     * @return true if the first result is a ResultSet object; false if it is an update count or there are no results
     * @throws SQLException if there is any database error or this method is called on a closed Statement
     */
    int executeUpdate(Statement statement, QueryTemplate queryTemplate) throws SQLException;
}
