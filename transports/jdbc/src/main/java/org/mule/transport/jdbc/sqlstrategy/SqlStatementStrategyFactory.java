/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.sqlstrategy;

/**
 * Factory that selects appropriate implementation of SQLStrategy for a particular SQL string
 */
public interface SqlStatementStrategyFactory
{
    SqlStatementStrategy create(String sql, Object payload) throws Exception;
}
