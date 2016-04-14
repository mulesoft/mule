/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.processor;

import static org.mule.api.debug.FieldDebugInfoFactory.createFieldDebugInfo;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.QUERIES_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.QUERY_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.createQueryFieldDebugInfo;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.executor.BulkExecutor;
import org.mule.module.db.internal.domain.executor.BulkQueryExecutorFactory;
import org.mule.module.db.internal.domain.query.BulkQuery;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.resolver.query.QueryResolutionException;
import org.mule.module.db.internal.resolver.query.QueryResolver;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Executes an update dynamic query in bulk mode on a database
 * * <p/>
 * A dynamic update query can be update, insert or delete query or a stored procedure
 * taking input parameters only and returning an update count.
 * <p/>
 * Both database and queries are resolved, if required, using the {@link org.mule.api.MuleEvent}
 * being processed.
 */
public class DynamicBulkUpdateMessageProcessor extends AbstractBulkUpdateMessageProcessor
{

    public DynamicBulkUpdateMessageProcessor(DbConfigResolver dbConfigResolver, QueryResolver queryResolver, BulkQueryExecutorFactory bulkUpdateExecutorFactory, TransactionalAction transactionalAction, List<QueryType> validQueryTypes)
    {
        super(dbConfigResolver, transactionalAction, validQueryTypes, queryResolver, bulkUpdateExecutorFactory);
    }

    @Override
    protected Object executeQuery(DbConnection connection, MuleEvent muleEvent) throws SQLException
    {
        BulkQuery bulkQuery = resolveBulkQuery(connection, muleEvent);
        BulkExecutor bulkUpdateExecutor = bulkUpdateExecutorFactory.create();

        return bulkUpdateExecutor.execute(connection, bulkQuery);
    }

    private BulkQuery resolveBulkQuery(DbConnection connection, MuleEvent muleEvent)
    {
        final Iterator<Object> paramsIterator = getIterator(muleEvent);

        BulkQuery bulkQuery = new BulkQuery();
        while (paramsIterator.hasNext())
        {
            MuleMessage itemMessage = new DefaultMuleMessage(paramsIterator.next(), muleContext);
            MuleEvent itemEvent = new DefaultMuleEvent(itemMessage, muleEvent);
            Query query = queryResolver.resolve(connection, itemEvent);
            bulkQuery.add(query.getQueryTemplate());
        }
        return bulkQuery;
    }

    @Override
    protected List<FieldDebugInfo<?>> getMessageProcessorDebugInfo(DbConnection connection, MuleEvent muleEvent)
    {
        MuleEvent eventToUse = resolveSource(muleEvent);
        final List<FieldDebugInfo<?>> fields = new ArrayList<>();

        BulkQuery bulkQuery;
        try
        {
            bulkQuery = resolveBulkQuery(connection, eventToUse);
        }
        catch (QueryResolutionException e)
        {
            fields.add(createFieldDebugInfo(QUERIES_DEBUG_FIELD, List.class, e));
            return fields;
        }

        final List<FieldDebugInfo<?>> queries = new ArrayList<>();

        int queryIndex = 1;
        for (QueryTemplate queryTemplate : bulkQuery.getQueryTemplates())
        {
            final String name = QUERY_DEBUG_FIELD + queryIndex++;
            final FieldDebugInfo queryFieldDebugInfo = createQueryFieldDebugInfo(name, queryTemplate);

            queries.add(queryFieldDebugInfo);
        }

        fields.add(createFieldDebugInfo(QUERIES_DEBUG_FIELD, List.class, queries));

        return fields;
    }
}
