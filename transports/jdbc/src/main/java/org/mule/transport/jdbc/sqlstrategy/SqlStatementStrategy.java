/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.sqlstrategy;

/**
 * Interface for classes implementing strategies for handling SQL statements.
 *
* @see DefaultSqlStatementStrategyFactory
 */

import java.sql.Connection;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.jdbc.JdbcConnector;

public interface SqlStatementStrategy 
{
    
    public MuleMessage executeStatement(JdbcConnector connector,
                                        ImmutableEndpoint endpoint,
                                        MuleEvent event,
                                        long timeout, Connection connection) throws Exception;
    
}
