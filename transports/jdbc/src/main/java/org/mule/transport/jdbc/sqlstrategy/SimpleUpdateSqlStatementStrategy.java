/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.sqlstrategy;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transaction.Transaction;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.jdbc.JdbcConnector;
import org.mule.transport.jdbc.JdbcUtils;
import org.mule.util.ArrayUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Implements strategy for handling individual insert, update, and delete statements
 */
public  class SimpleUpdateSqlStatementStrategy implements SqlStatementStrategy
{
    protected transient Logger logger = Logger.getLogger(getClass());

    @Override
    public MuleMessage executeStatement(JdbcConnector connector,
                                        ImmutableEndpoint endpoint, MuleEvent event, long timeout, Connection connection) throws Exception
    {
        //Unparsed SQL statement (with #[foo] format parameters)
        String statement = connector.getStatement(endpoint);

        //Storage for parameters
        List<?> paramNames = new ArrayList<Object>();

        //Parsed SQL statement (with ? placeholders instead of #[foo] params)
        String sql = connector.parseStatement(statement, paramNames);

        //Optionally escape or further manipulate SQL statement.  Used in subclasses.
        sql = escapeStatement(sql);

        //Get parameter values from message
        MuleMessage message = event.getMessage();
        Object[] paramValues = connector.getParams(endpoint, paramNames, new DefaultMuleMessage(
            event.getMessage().getPayload(), message, event.getMuleContext()), endpoint.getEndpointURI().getAddress());

        if (logger.isDebugEnabled())
        {
            logger.debug("SQL UPDATE: " + sql + ", params = " + ArrayUtils.toString(paramValues));
        }

        int nbRows = connector.getQueryRunnerFor(endpoint).update(connection, sql, paramValues);
        if (logger.isInfoEnabled())
        {
            logger.info("Executing SQL statement: " + nbRows + " row(s) updated");
        }

        // TODO Why should it always be 1?  Can't we update more than one row at a time with
        // an update statement?  Or no rows depending on the contents of the table and/or
        // parameters?
        //if (nbRows != 1)
        //{
        //    logger.warn("Row count for write should be 1 and not " + nbRows);
        //}
        logger.debug("MuleEvent dispatched succesfuly");
        return event.getMessage();
    }

    protected String escapeStatement(String statement)
    {
        //no escaping needed for normal SQL statement
        return statement;
    }
}
