/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.statement;

import org.mule.module.db.internal.result.resultset.ResultSetHandler;

import java.util.Map;

/**
 * Processes {@link java.sql.Statement} results without closing the statement.
 */
public class StreamingStatementResultHandler extends AbstractMapStatementResultHandler
{

    public StreamingStatementResultHandler(ResultSetHandler resultSetHandler)
    {
        super(resultSetHandler);
    }

    @Override
    protected Map<String, Object> createResultMap()
    {
        return new CloseableMap<String, Object>();
    }
}
