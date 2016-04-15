/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.logger;

/**
 * Logs a bulk query
 */
public interface BulkQueryLogger extends QueryLogger
{

    /**
     * Adds a new query to log
     *
     * @param query query added to the bulk
     */
    void addQuery(String query);
}
