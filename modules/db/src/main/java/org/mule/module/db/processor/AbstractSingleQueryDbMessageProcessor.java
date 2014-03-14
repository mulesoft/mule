/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.processor;

import org.mule.api.MuleEvent;
import org.mule.module.db.resolver.query.QueryResolver;
import org.mule.module.db.domain.query.Query;
import org.mule.module.db.resolver.database.DbConfigResolver;
import org.mule.module.db.domain.connection.DbConnection;
import org.mule.module.db.domain.transaction.TransactionalAction;

import java.sql.SQLException;

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

        Query resolvedQuery = queryResolver.resolve(eventToUse);

        validateQueryType(resolvedQuery.getQueryTemplate());

        return doExecuteQuery(connection, resolvedQuery);
    }

    protected abstract Object doExecuteQuery(DbConnection connection, Query query) throws SQLException;
}
