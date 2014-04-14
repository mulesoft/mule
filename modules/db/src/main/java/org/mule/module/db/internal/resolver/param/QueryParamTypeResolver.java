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

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves parameter types for standard queries
 */
public class QueryParamTypeResolver implements ParamTypeResolver
{

    private final DbTypeManager dbTypeManager;

    public QueryParamTypeResolver(DbTypeManager dbTypeManager)
    {
        this.dbTypeManager = dbTypeManager;
    }

    @Override
    public Map<Integer, DbType> getParameterTypes(DbConnection connection, QueryTemplate queryTemplate) throws SQLException
    {
        Map<Integer, DbType> paramTypes = new HashMap<Integer, DbType>();

        PreparedStatement statement = connection.prepareStatement(queryTemplate.getSqlText());

        ParameterMetaData parameterMetaData = statement.getParameterMetaData();

        for (QueryParam queryParam : queryTemplate.getParams())
        {
            int parameterTypeId = parameterMetaData.getParameterType(queryParam.getIndex());
            String parameterTypeName = parameterMetaData.getParameterTypeName(queryParam.getIndex());
            DbType dbType = dbTypeManager.lookup(connection, parameterTypeId, parameterTypeName);
            paramTypes.put(queryParam.getIndex(), dbType);
        }

        return paramTypes;
    }
}
