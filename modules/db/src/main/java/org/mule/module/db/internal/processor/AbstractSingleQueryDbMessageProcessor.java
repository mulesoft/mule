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
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.SQL_TEXT_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.TYPE_DEBUG_FIELD;
import org.mule.api.MuleEvent;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.resolver.query.QueryResolutionException;
import org.mule.module.db.internal.resolver.query.QueryResolver;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for message processors processing only one query
 */
public abstract class AbstractSingleQueryDbMessageProcessor extends AbstractDbMessageProcessor
{

    private final QueryResolver queryResolver;

    public AbstractSingleQueryDbMessageProcessor(DbConfigResolver dbConfigResolver, QueryResolver queryResolver, TransactionalAction transactionalAction)
    {
        super(dbConfigResolver, transactionalAction);
        this.queryResolver = queryResolver;
    }

    @Override
    protected Object executeQuery(DbConnection connection, MuleEvent muleEvent) throws SQLException
    {
        MuleEvent eventToUse = resolveSource(muleEvent);

        Query resolvedQuery = queryResolver.resolve(connection, eventToUse);

        validateQueryType(resolvedQuery.getQueryTemplate());

        return doExecuteQuery(connection, resolvedQuery);
    }

    protected abstract Object doExecuteQuery(DbConnection connection, Query query) throws SQLException;

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
            fields.add(createFieldDebugInfo(SQL_TEXT_DEBUG_FIELD, String.class, e));
            return fields;
        }

        fields.add(createFieldDebugInfo(SQL_TEXT_DEBUG_FIELD, String.class, resolvedQuery.getQueryTemplate().getSqlText()));
        fields.add(createFieldDebugInfo(TYPE_DEBUG_FIELD, String.class, resolvedQuery.getQueryTemplate().getType().toString()));

        final List<FieldDebugInfo<?>> paramFields = new ArrayList<>();
        int paramIndex = 1;
        for (QueryParamValue queryParamValue : resolvedQuery.getParamValues())
        {
            final String name = queryParamValue.getName() == null ? PARAM_DEBUG_FIELD_PREFIX + paramIndex++ : queryParamValue.getName();
            paramFields.add(createFieldDebugInfo(name, String.class, queryParamValue.getValue()));
        }
        fields.add(createFieldDebugInfo(INPUT_PARAMS_DEBUG_FIELD, List.class, paramFields));

        return fields;
    }
}
