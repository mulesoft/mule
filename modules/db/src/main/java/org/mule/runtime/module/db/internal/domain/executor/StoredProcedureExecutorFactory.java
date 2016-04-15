/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.executor;

import org.mule.module.db.internal.result.statement.StatementResultHandler;
import org.mule.module.db.internal.domain.statement.StatementFactory;

/**
 * Creates {@link StoredProcedureExecutor} instances
 */
public class StoredProcedureExecutorFactory implements QueryExecutorFactory
{

    private final StatementFactory statementFactory;
    private final StatementResultHandler statementResultHandler;

    public StoredProcedureExecutorFactory(StatementFactory statementFactory, StatementResultHandler statementResultHandler)
    {
        this.statementFactory = statementFactory;
        this.statementResultHandler = statementResultHandler;
    }

    @Override
    public QueryExecutor create()
    {
        return new StoredProcedureExecutor(statementFactory, statementResultHandler);
    }
}
