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
 * Creates {@link QueryLogger} instances
 */
public interface QueryLoggerFactory
{

    /**
     * Creates a logger for a single query
     *
     * @param logger logger where the query will be logged
     * @param queryTemplate query tepmlate to log
     * @return a non null {@link SingleQueryLogger}
     */
    SingleQueryLogger createQueryLogger(Log logger, QueryTemplate queryTemplate);

    /**
     * Creates a logger for a single query running in bulk mode
     *
     * @param logger logger where the query will be logged
     * @param queryTemplate query tepmlate to log
     * @param bulkSize total size of the bulk operation. Must be positive
     * @return a non null {@link PreparedBulkQueryLogger}
     */
    PreparedBulkQueryLogger createBulkQueryLogger(Log logger, QueryTemplate queryTemplate, int bulkSize);

    /**
     * Creates a logger for a bulk query
     *
     * @param logger logger where the query will be logged
     * @return a non null {@link BulkQueryLogger}
     */
    BulkQueryLogger createBulkQueryLogger(Log logger);
}
