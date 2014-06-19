/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.processor;

import org.mule.module.db.internal.resolver.query.QueryResolver;
import org.mule.module.db.internal.domain.executor.QueryExecutor;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.executor.QueryExecutorFactory;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes a DDL query on a database
 * <p/>
 * Both database and bulk query are resolved, if required, using the {@link org.mule.api.MuleEvent}
 * being processed.
 */
public class ExecuteDdlMessageProcessor extends AbstractSingleQueryDbMessageProcessor
{

    private final QueryExecutorFactory queryExecutorFactory;
    private final List<QueryType> validQueryTypes;

    public ExecuteDdlMessageProcessor(DbConfigResolver dbConfigResolver, QueryResolver queryResolver, QueryExecutorFactory queryExecutorFactory, TransactionalAction transactionalAction)
    {
        super(dbConfigResolver, queryResolver, transactionalAction);
        this.queryExecutorFactory = queryExecutorFactory;
        validQueryTypes = new ArrayList<QueryType>();
        validQueryTypes.add(QueryType.DDL);
    }

    @Override
    protected List<QueryType> getValidQueryTypes()
    {
        return validQueryTypes;
    }

    @Override
    protected Object doExecuteQuery(DbConnection connection, Query query) throws SQLException
    {
        QueryExecutor queryExecutor = queryExecutorFactory.create();
        return queryExecutor.execute(connection, query);
    }
}
