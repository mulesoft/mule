/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database data type
 */
public interface DbType
{

    /**
     * @return data type ID
     */
    int getId();

    /**
     * @return data type name
     */
    String getName();

    /**
     * Sets the value of an input parameter
     *
     * @param statement statement that contains the parameter
     * @param index index of the parameter in the statement (first parameter is 1, the second is 2, etc)
     * @param value value to assign
     * @throws SQLException if parameterIndex does not correspond to a parameter marker in the SQL statement;
     * if a database access error occurs; this method is called on a closed PreparedStatement or the type of
     * the given object is ambiguous
     */
    void setParameterValue(PreparedStatement statement, int index, Object value) throws SQLException;

    /**
     * Gets the value of an output parameter
     *
     * @param statement statement that contains the parameter
     * @param index index of the parameter in the statement (first parameter is 1, the second is 2, etc)
     * @throws SQLException if parameterIndex does not correspond to a parameter marker in the SQL statement;
     * if a database access error occurs; this method is called on a closed statement
     */
    Object getParameterValue(CallableStatement statement, int index) throws SQLException;

    /**
     * Registers an output parameter
     *
     * @param statement statement that contains the parameter
     * @param index index of the parameter in the statement (first parameter is 1, the second is 2, etc)
     * @throws SQLException if the parameterIndex is not valid; if a database access error occurs or this method
     * is called on a closed CallableStatement
     */
    void registerOutParameter(CallableStatement statement, int index) throws SQLException;
}
