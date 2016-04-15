/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.statement;

import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.result.resultset.StreamingResultSetCloser;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for implementing {@link StreamingResultSetCloser}
 */
public class AbstractStreamingResultSetCloser implements StreamingResultSetCloser
{

    protected static final Log logger = LogFactory.getLog(AbstractStreamingResultSetCloser.class);

    @Override
    public void close(DbConnection connection, ResultSet resultSet)
    {
        try
        {
            resultSet.close();
        }
        catch (SQLException e)
        {
            logger.warn("Error attempting to close resultSet", e);
        }
    }
}
