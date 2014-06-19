/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.executor;

/**
 * Creates {@link BulkExecutor} instances
 */
public interface BulkQueryExecutorFactory
{

    /**
     * @return a non null bulk query executor
     */
    BulkExecutor create();
}
