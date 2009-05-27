/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.sqlstrategy;

/**
 * Interface for classes implementing strategies for handling SQL statements.
 *
* @see DefaultSqlStatementStrategyFactory
 */

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.jdbc.JdbcConnector;

public interface SqlStatementStrategy 
{
    
    public MuleMessage executeStatement(JdbcConnector connector,
                                         ImmutableEndpoint endpoint,
                                         MuleEvent event,
                                         long timeout) throws Exception;
    
}
