/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.resolver.query;

import org.mule.api.MuleEvent;
import org.mule.module.db.domain.connection.DbConnection;
import org.mule.module.db.domain.database.DbConfig;
import org.mule.module.db.domain.param.DefaultInOutQueryParam;
import org.mule.module.db.domain.param.DefaultInputQueryParam;
import org.mule.module.db.domain.param.DefaultOutputQueryParam;
import org.mule.module.db.domain.param.InOutQueryParam;
import org.mule.module.db.domain.param.InputQueryParam;
import org.mule.module.db.domain.param.OutputQueryParam;
import org.mule.module.db.domain.param.QueryParam;
import org.mule.module.db.domain.query.Query;
import org.mule.module.db.domain.query.QueryParamValue;
import org.mule.module.db.domain.query.QueryTemplate;
import org.mule.module.db.domain.transaction.TransactionalAction;
import org.mule.module.db.domain.type.DbType;
import org.mule.module.db.resolver.database.DbConfigResolver;
import org.mule.module.db.resolver.param.QueryParamResolver;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves a stored procedure query evaluating parameter value expression using a given event
 */
public class StoredProcedureQueryResolver implements QueryResolver
{

    private final Query query;
    private final QueryParamResolver queryParamResolver;
    private final DbConfigResolver dbConfigResolver;

    public StoredProcedureQueryResolver(Query query, DbConfigResolver dbConfigResolver, QueryParamResolver queryParamResolver)
    {
        this.query = query;
        this.dbConfigResolver = dbConfigResolver;
        this.queryParamResolver = queryParamResolver;
    }

    @Override
    public Query resolve(MuleEvent muleEvent)
    {
        if (muleEvent == null)
        {
            return query;
        }

        List<QueryParamValue> resolvedParams = queryParamResolver.resolveParams(muleEvent, query.getParamValues());

        QueryTemplate queryTemplate = resolveStoredProcedureDefinition(muleEvent, query);

        return new Query(queryTemplate, resolvedParams);
    }

    private QueryTemplate resolveStoredProcedureDefinition(MuleEvent muleEvent, Query query)
    {
        QueryTemplate queryTemplate = query.getQueryTemplate();

        DbConfig config = dbConfigResolver.resolve(muleEvent);

        Map<Integer, DbType> paramTypes = getStoreProcedureParameterTypes(queryTemplate, config);

        return resolveQueryTemplate(queryTemplate, paramTypes);
    }

    private Map<Integer, DbType> getStoreProcedureParameterTypes(QueryTemplate originalQuery, DbConfig config)
    {
        DbConnection connection = null;

        try
        {
            connection = config.getConnectionFactory().createConnection(TransactionalAction.JOIN_IF_POSSIBLE);
            return config.getDbTypeResolver().getParameterTypes(connection, originalQuery);
        }
        catch (SQLException e)
        {
            throw new QueryResolutionException("Unable to resolve query " + originalQuery.getSqlText(), e);
        }
        finally
        {
            config.getConnectionFactory().releaseConnection(connection);
        }
    }

    private QueryTemplate resolveQueryTemplate(QueryTemplate queryTemplate, Map<Integer, DbType> paramTypes)
    {
        List<QueryParam> newParams = new ArrayList<QueryParam>();

        for (QueryParam originalParam : queryTemplate.getParams())
        {
            DbType type = paramTypes.get((originalParam).getIndex());
            QueryParam newParam;

            if (originalParam instanceof InOutQueryParam)
            {
                newParam = new DefaultInOutQueryParam(originalParam.getIndex(), type, originalParam.getName(), ((InOutQueryParam) originalParam).getValue());
            }
            else if (originalParam instanceof InputQueryParam)
            {
                newParam = new DefaultInputQueryParam(originalParam.getIndex(), type, ((InputQueryParam) originalParam).getValue(), originalParam.getName());
            }
            else if (originalParam instanceof OutputQueryParam)
            {
                newParam = new DefaultOutputQueryParam(originalParam.getIndex(), type, originalParam.getName());
            }
            else
            {
                throw new IllegalArgumentException("Unknown parameter type: " + originalParam.getClass().getName());

            }

            newParams.add(newParam);
        }

        return new QueryTemplate(queryTemplate.getSqlText(), queryTemplate.getType(), newParams);
    }
}