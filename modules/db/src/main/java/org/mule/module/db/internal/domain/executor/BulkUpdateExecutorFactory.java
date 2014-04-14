/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.executor;

import org.mule.module.db.internal.domain.statement.StatementFactory;

/**
 * Creates {@link BulkUpdateExecutor} instances
 */
public class BulkUpdateExecutorFactory implements BulkQueryExecutorFactory
{

    private final StatementFactory statementFactory;

    public BulkUpdateExecutorFactory(StatementFactory statementFactory)
    {
        this.statementFactory = statementFactory;
    }

    @Override
    public BulkUpdateExecutor create()
    {
        return new BulkUpdateExecutor(statementFactory);
    }
}
