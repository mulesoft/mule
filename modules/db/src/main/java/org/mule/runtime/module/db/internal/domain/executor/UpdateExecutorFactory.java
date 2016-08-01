/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.internal.domain.executor;

import org.mule.runtime.module.db.internal.domain.statement.StatementFactory;

/**
 * Creates {@link UpdateExecutor} instances
 */
public class UpdateExecutorFactory implements QueryExecutorFactory
{

    private final StatementFactory statementFactory;

    public UpdateExecutorFactory(StatementFactory statementFactory)
    {
        this.statementFactory = statementFactory;
    }

    @Override
    public QueryExecutor create()
    {
        return new UpdateExecutor(statementFactory);
    }
}
