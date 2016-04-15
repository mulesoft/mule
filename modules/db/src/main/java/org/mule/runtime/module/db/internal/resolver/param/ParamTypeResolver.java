/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.param;

import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.UnknownDbTypeException;

import java.sql.SQLException;
import java.util.Map;

/**
 * Resolves {@link QueryTemplate} actual parameter types for a given {@link DbConnection}
 */
public interface ParamTypeResolver
{

    /**
     * Resolves actual parameter types
     *
     * @param connection database connection to resolve against to
     * @param queryTemplate query template containing UNKNOWN parameter types
     * @return a map containing the actual type for each parameter index
     * @throws SQLException if this method is invoked on a closed connection
     * @throws UnknownDbTypeException when an invalid data type is used
     */
    Map<Integer, DbType> getParameterTypes(DbConnection connection, QueryTemplate queryTemplate) throws SQLException;
}
