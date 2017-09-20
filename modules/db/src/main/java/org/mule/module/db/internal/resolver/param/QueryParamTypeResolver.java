/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.param;

import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.internal.domain.type.ResolvedDbType;
import org.mule.module.db.internal.domain.type.UnknownDbType;
import org.mule.module.db.internal.domain.type.UnknownDbTypeException;

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves parameter types for standard queries
 */
public class QueryParamTypeResolver implements ParamTypeResolver
{
    private static final Logger logger = LoggerFactory.getLogger(QueryParamTypeResolver.class);

    private final DbTypeManager dbTypeManager;

    public QueryParamTypeResolver(DbTypeManager dbTypeManager)
    {
        this.dbTypeManager = dbTypeManager;
    }

    @Override
    public Map<Integer, DbType> getParameterTypes(DbConnection connection, QueryTemplate queryTemplate) throws SQLException
    {
        Map<Integer, DbType> paramTypes = new HashMap<Integer, DbType>();

        PreparedStatement statement = null;
        try {

            statement = connection.prepareStatement(queryTemplate.getSqlText());

            ParameterMetaData parameterMetaData = statement.getParameterMetaData();

            for (QueryParam queryParam : queryTemplate.getParams())
            {
                int parameterTypeId = parameterMetaData.getParameterType(queryParam.getIndex());
                String parameterTypeName = parameterMetaData.getParameterTypeName(queryParam.getIndex());
                DbType dbType;
                if (parameterTypeName == null)
                {
                    // Use unknown data type
                    dbType = UnknownDbType.getInstance();
                }
                else
                {
                    try
                    {
                        dbType = dbTypeManager.lookup(connection, parameterTypeId, parameterTypeName);
                    } catch (UnknownDbTypeException e) {
                        // Type was not found in the type manager, but the DB knows about it
                        dbType = new ResolvedDbType(parameterTypeId, parameterTypeName);
                    }
                }

                paramTypes.put(queryParam.getIndex(), dbType);
            }
        }
        finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException e)
                {
                    if (logger.isWarnEnabled())
                    {
                        logger.warn("Could not close statement", e);
                    }
                }
            }
        }

        return paramTypes;
    }
}
