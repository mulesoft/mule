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
import org.mule.module.db.internal.domain.type.DynamicDbType;
import org.mule.module.db.internal.domain.type.UnknownDbType;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves unknown and dynamic types using metadata if possible
 */
public class DefaultParamTypeResolver implements ParamTypeResolver
{

    protected final DbTypeManager dbTypeManager;
    private ParamTypeResolver metadataParamTypeResolver;

    protected DefaultParamTypeResolver(DbTypeManager dbTypeManager, ParamTypeResolver metadataParamTypeResolver)
    {
        this.dbTypeManager = dbTypeManager;
        this.metadataParamTypeResolver = metadataParamTypeResolver;
    }

    public Map<Integer, DbType> getParameterTypes(DbConnection connection, QueryTemplate queryTemplate) throws SQLException
    {
        Map<Integer, DbType> resolvedParamTypes = new HashMap<Integer, DbType>();

        Map<Integer, DbType> metadataParamTypes = null;

        for (QueryParam queryParam : queryTemplate.getParams())
        {
            if (queryParam.getType() instanceof UnknownDbType)
            {
                if (metadataParamTypes == null)
                {
                    metadataParamTypes = getParamTypesUsingMetadata(connection, queryTemplate);
                }

                resolvedParamTypes.put(queryParam.getIndex(), metadataParamTypes.get(queryParam.getIndex()));
            }
            else if (queryParam.getType() instanceof DynamicDbType)
            {
                DbType dbType = dbTypeManager.lookup(connection, queryParam.getType().getName());

                resolvedParamTypes.put(queryParam.getIndex(), dbType);
            }
            else
            {
                resolvedParamTypes.put(queryParam.getIndex(), queryParam.getType());
            }
        }

        return resolvedParamTypes;
    }

    protected Map<Integer, DbType> getParamTypesUsingMetadata(DbConnection connection, QueryTemplate queryTemplate)
    {
        Map<Integer, DbType> metadataParamTypes;
        try
        {
            metadataParamTypes = metadataParamTypeResolver.getParameterTypes(connection, queryTemplate);
        }
        catch (SQLException e)
        {
            metadataParamTypes = getParamTypesFromQueryTemplate(queryTemplate);
        }
        return metadataParamTypes;
    }

    private Map<Integer, DbType> getParamTypesFromQueryTemplate(QueryTemplate queryTemplate)
    {
        Map<Integer, DbType> paramTypes = new HashMap<Integer, DbType>();

        for (QueryParam queryParam : queryTemplate.getParams())
        {
            paramTypes.put(queryParam.getIndex(), queryParam.getType());
        }

        return paramTypes;
    }
}
