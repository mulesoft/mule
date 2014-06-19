/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.logger;

import org.mule.module.db.internal.domain.query.QueryTemplate;

import org.apache.commons.logging.Log;

/**
 * Logs a prepared bulk query in debug level
 */
public class DebugPreparedBulkQueryLogger extends DebugSingleQueryLogger implements PreparedBulkQueryLogger
{

    public static final String PARAMETER_SET_BEGIN = "\n{";
    public static final String PARAMETER_SET_END = "\n}";

    private final int bulkSize;
    private int currentBulkSize = 0;

    public DebugPreparedBulkQueryLogger(Log logger, QueryTemplate queryTemplate, int bulkSize)
    {
        super(logger, queryTemplate);
        this.bulkSize = bulkSize;

        if (hasParameters())
        {
            builder.append(PARAMETER_SET_BEGIN);
        }
    }

    @Override
    public void addParameterSet()
    {
        currentBulkSize++;

        if (hasParameters())
        {
            builder.append(PARAMETER_SET_END);
            if (currentBulkSize < bulkSize)
            {
                builder.append(PARAMETER_SET_BEGIN);
            }
        }

    }
}
