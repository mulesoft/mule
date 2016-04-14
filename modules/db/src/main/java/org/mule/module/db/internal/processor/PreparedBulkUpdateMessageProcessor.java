/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.processor;

import static org.mule.api.debug.FieldDebugInfoFactory.createFieldDebugInfo;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.INPUT_PARAMS_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.PARAM_DEBUG_FIELD_PREFIX;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.PARAM_SET_DEBUG_FIELD_PREFIX;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.QUERY_DEBUG_FIELD;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.executor.BulkExecutor;
import org.mule.module.db.internal.domain.executor.BulkQueryExecutorFactory;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.resolver.param.ParamValueResolver;
import org.mule.module.db.internal.resolver.query.QueryResolutionException;
import org.mule.module.db.internal.resolver.query.QueryResolver;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Executes an update query in bulk mode on a database
 * * <p/>
 * An update query can be parametrized update, insert or delete query or a stored procedure
 * taking input parameters only and returning an update count.
 * <p/>
 * Both database and queries are resolved, if required, using the {@link org.mule.api.MuleEvent}
 * being processed.
 */
public class PreparedBulkUpdateMessageProcessor extends AbstractBulkUpdateMessageProcessor
{

    private final ParamValueResolver paramValueResolver;

    public PreparedBulkUpdateMessageProcessor(DbConfigResolver dbConfigResolver, QueryResolver queryResolver, BulkQueryExecutorFactory bulkUpdateExecutorFactory, TransactionalAction transactionalAction, List<QueryType> validQueryTypes, ParamValueResolver paramValueResolver)
    {
        super(dbConfigResolver, transactionalAction, validQueryTypes, queryResolver, bulkUpdateExecutorFactory);
        this.paramValueResolver = paramValueResolver;
    }

    @Override
    protected Object executeQuery(DbConnection connection, MuleEvent muleEvent) throws SQLException
    {
        Query query = queryResolver.resolve(connection, muleEvent);

        validateQueryType(query.getQueryTemplate());

        List<List<QueryParamValue>> paramValues = resolveParamSets(muleEvent, query);

        BulkExecutor bulkUpdateExecutor = bulkUpdateExecutorFactory.create();
        return bulkUpdateExecutor.execute(connection, query, paramValues);
    }

    private List<List<QueryParamValue>> resolveParamSets(MuleEvent muleEvent, Query query)
    {
        final Iterator<Object> paramsIterator = getIterator(muleEvent);

        List<List<QueryParamValue>> result = new LinkedList<>();

        while (paramsIterator.hasNext())
        {
            MuleMessage itemMessage = new DefaultMuleMessage(paramsIterator.next(), muleContext);
            MuleEvent itemEvent = new DefaultMuleEvent(itemMessage, muleEvent);
            List<QueryParamValue> queryParamValues = paramValueResolver.resolveParams(itemEvent, query.getParamValues());
            result.add(queryParamValues);
        }

        return result;
    }

    @Override
    protected List<FieldDebugInfo<?>> getMessageProcessorDebugInfo(DbConnection connection, MuleEvent muleEvent)
    {
        MuleEvent eventToUse = resolveSource(muleEvent);
        final List<FieldDebugInfo<?>> fields = new ArrayList<>();

        Query resolvedQuery;
        try
        {
            resolvedQuery = queryResolver.resolve(connection, eventToUse);
        }
        catch (QueryResolutionException e)
        {
            fields.add(createFieldDebugInfo(QUERY_DEBUG_FIELD, String.class, e));

            return fields;
        }

        fields.add(DbDebugInfoUtils.createQueryFieldDebugInfo(QUERY_DEBUG_FIELD, resolvedQuery.getQueryTemplate()));

        final List<List<QueryParamValue>> paramSets = resolveParamSets(muleEvent, resolvedQuery);

        List<FieldDebugInfo<?>> paramSetInfos = new LinkedList<>();
        int setIndex = 1;
        for (List<QueryParamValue> paramSet : paramSets)
        {
            final List<FieldDebugInfo<?>> paramFields = new ArrayList<>();
            int paramIndex = 1;
            for (QueryParamValue paramValue : paramSet)
            {
                final String name = paramValue.getName() == null ? PARAM_DEBUG_FIELD_PREFIX + paramIndex++ : paramValue.getName();
                paramFields.add(createFieldDebugInfo(name, String.class, paramValue.getValue()));
            }
            paramSetInfos.add(createFieldDebugInfo(PARAM_SET_DEBUG_FIELD_PREFIX + setIndex++, List.class, paramFields));
        }
        fields.add(createFieldDebugInfo(INPUT_PARAMS_DEBUG_FIELD, List.class, paramSetInfos));

        return fields;
    }
}
