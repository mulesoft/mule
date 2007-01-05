/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms;

import oracle.jdbc.driver.OracleDriver;
import oracle.jdbc.pool.OracleDataSource;

import org.mule.umo.lifecycle.InitialisationException;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.jms.JMSException;

/**
 * Extends the standard Mule JMS Provider with functionality specific to Oracle's JMS
 * implementation based on Advanced Queueing (Oracle AQ).
 * 
 * @see OracleJmsSupport
 * @see org.mule.providers.jms.JmsConnector
 * @see <a href="http://otn.oracle.com/pls/db102/">Streams Advanced Queuing</a>
 */
public class OracleJmsConnector extends AbstractOracleJmsConnector
{

    /**
     * The JDBC URL for the Oracle database. For example,
     * {@code jdbc:oracle:oci:@myhost}
     */
    private String url;

    /**
     * Since many connections are opened and closed, we use a connection pool to
     * obtain the JDBC connection.
     */
    private OracleDataSource jdbcConnectionPool = null;

    public OracleJmsConnector()
    {
        super();

    }

    protected void doInitialise() throws InitialisationException
    {
        try
        {
            // Register the Oracle JDBC driver.
            Driver oracleDriver = new OracleDriver();
            // Deregister first just in case the driver has already been registered.
            DriverManager.deregisterDriver(oracleDriver);
            DriverManager.registerDriver(oracleDriver);

            jdbcConnectionPool = new OracleDataSource();
            jdbcConnectionPool.setDataSourceName("Mule Oracle AQ Provider");
            jdbcConnectionPool.setUser(username);
            jdbcConnectionPool.setPassword(password);
            jdbcConnectionPool.setURL(url);

        }
        catch (SQLException e)
        {
            throw new InitialisationException(e, this);
        }
        super.doInitialise();
    }

    public java.sql.Connection getJdbcConnection() throws JMSException
    {
        try
        {
            logger.debug("Getting queue/topic connection from pool, URL = "
                         + getJdbcConnectionPool().getURL() + ", user = " + getJdbcConnectionPool().getUser());
            return getJdbcConnectionPool().getConnection();
        }
        catch (SQLException e)
        {
            throw new JMSException("Unable to open JDBC connection: " + e.getMessage());
        }
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public OracleDataSource getJdbcConnectionPool()
    {
        return jdbcConnectionPool;
    }

}
