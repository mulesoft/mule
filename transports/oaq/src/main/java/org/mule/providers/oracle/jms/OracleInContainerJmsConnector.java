/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms;

import java.sql.Connection;
import java.sql.SQLException;

import javax.jms.JMSException;
import javax.sql.DataSource;

/**
 * JMS Connector for Oracle AQ that uses a user provided data source for database connectivity
 */
public class OracleInContainerJmsConnector extends AbstractOracleJmsConnector
{

    private DataSource dataSource;

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public Connection getJdbcConnection() throws JMSException
    {
        try
        {
            return dataSource.getConnection();
        }
        catch (SQLException e)
        {
            throw new JMSException("Unable to open JDBC connection: " + e.getMessage());
        }
    }

}
