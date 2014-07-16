/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import org.mule.api.MuleEvent;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.param.DefaultInOutQueryParam;
import org.mule.module.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.module.db.internal.domain.param.DefaultOutputQueryParam;
import org.mule.module.db.internal.domain.param.InOutQueryParam;
import org.mule.module.db.internal.domain.param.InputQueryParam;
import org.mule.module.db.internal.domain.param.OutputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.DynamicDbType;
import org.mule.module.db.internal.domain.type.UnknownDbType;
import org.mule.module.db.internal.resolver.param.ParamValueResolver;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves a parameterized query evaluating parameter value expression using a given event
 */
public class ParametrizedQueryResolver implements QueryResolver
{

    private final Query query;
    private final ParamValueResolver paramValueResolver;

    public ParametrizedQueryResolver(Query query, ParamValueResolver paramValueResolver)
    {
        this.query = query;
        this.paramValueResolver = paramValueResolver;
    }

    @Override
    public Query resolve(DbConnection connection, MuleEvent muleEvent)
    {
        List<QueryParamValue> resolvedParams = paramValueResolver.resolveParams(muleEvent, query.getParamValues());

        QueryTemplate queryTemplate = query.getQueryTemplate();

        if (needsParamTypeResolution(queryTemplate.getParams()))
        {
            Map<Integer, DbType> paramTypes = getParameterTypes(connection, queryTemplate);

            queryTemplate = resolveQueryTemplate(queryTemplate, paramTypes);
        }

        return new Query(queryTemplate, resolvedParams);
    }

    private Map<Integer, DbType> getParameterTypes(DbConnection connection, QueryTemplate queryTemplate)
    {
        try
        {
            return connection.getParamTypes(queryTemplate);
        }
        catch (SQLException e)
        {
            throw new QueryResolutionException("Cannot resolve parameter types", e);
        }
    }

    private boolean needsParamTypeResolution(List<QueryParam> params)
    {
        for (QueryParam param : params)
        {
            if (param.getType() == UnknownDbType.getInstance() || param.getType() instanceof DynamicDbType)
            {
                return true;
            }
        }

        return false;
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