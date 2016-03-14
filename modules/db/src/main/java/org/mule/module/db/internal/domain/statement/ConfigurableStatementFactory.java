/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.statement;

/**
 * Provides extra customization to a {@link StatementFactory}
 */
public interface ConfigurableStatementFactory extends StatementFactory
{

    /**
     * Sets the maximum numbers of rows that will be returned by any resultSet
     * returned by a statement created by this factory
     *
     * @param max the new max rows limit; zero means there is no limit
     */
    void setMaxRows(int max);

    /**
     * Indicates how many rows should fetched from the database any time more
     * rows are requested on any resultSet returned by a statement created by
     * this factory
     *
     * @param size the number of rows to fetch
     */
    void setFetchSize(int size);

    /**
     * Indicates the minimum amount of time in seconds before the JDBC driver
     * attempts to cancel a running statement.
     *
     * @param queryTimeout number of seconds to wait. Non negative. Zero means no timeout.
     */
    void setQueryTimeout(int queryTimeout);
}
