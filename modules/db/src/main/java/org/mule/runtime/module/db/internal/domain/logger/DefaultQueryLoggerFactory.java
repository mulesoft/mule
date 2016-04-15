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
 * Creates query loggers with different implementations depending on whether a given {@link Log}
 * has the debug level enabled or not
 */
public class DefaultQueryLoggerFactory implements QueryLoggerFactory
{

    @Override
    public SingleQueryLogger createQueryLogger(Log logger, QueryTemplate queryTemplate)
    {
        if (logger.isDebugEnabled())
        {
            return new DebugSingleQueryLogger(logger, queryTemplate);
        }
        else
        {
            return new NullSingleQueryLogger();
        }
    }

    @Override
    public PreparedBulkQueryLogger createBulkQueryLogger(Log logger, QueryTemplate queryTemplate, int bulkSize)
    {
        if (logger.isDebugEnabled())
        {
            return new DebugPreparedBulkQueryLogger(logger, queryTemplate, bulkSize);
        }
        else
        {
            return new NullPreparedBulkQueryLogger();
        }
    }

    @Override
    public BulkQueryLogger createBulkQueryLogger(Log logger)
    {
        if (logger.isDebugEnabled())
        {
            return new DebugBulkQueryLogger(logger);
        }
        else
        {
            return new NullBulkQueryLogger();
        }
    }
}
