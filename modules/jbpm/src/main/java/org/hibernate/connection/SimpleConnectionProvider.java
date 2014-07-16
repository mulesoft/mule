/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.hibernate.connection;


import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.util.ReflectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the Hibernate class org.hibernate.connection.connection.DriverManagerConnectionProvider, 
 * tweaked in order to work with Mule's hot deployment.
 * 
 * A very simple connection provider which does <u>not</u> use <tt>java.sql.DriverManager</tt>. This provider
 * also implements a very rudimentary connection pool.
 * @see ConnectionProvider
 * @author Gavin King
 */
public class SimpleConnectionProvider implements ConnectionProvider {

    private String url;
    private Properties connectionProps;
    private Integer isolation;
    private final ArrayList pool = new ArrayList();
    private int poolSize;
    private int checkedOut = 0;
    private boolean autocommit;
    private Driver driver;

    private static final Logger log = LoggerFactory.getLogger(SimpleConnectionProvider.class);

    public void configure(Properties props) throws HibernateException {

        String driverClass = props.getProperty(Environment.DRIVER);

        poolSize = getInt(Environment.POOL_SIZE, props, 20); //default pool size 20
        log.info("Using Hibernate built-in connection pool (not for production use!)");
        log.info("Hibernate connection pool size: " + poolSize);

        autocommit = getBoolean(Environment.AUTOCOMMIT, props);
        log.info("autocommit mode: " + autocommit);

        isolation = getInteger(Environment.ISOLATION, props);
        if (isolation!=null)
        log.info( "JDBC isolation level: " + Environment.isolationLevelToString( isolation.intValue() ) );

        if (driverClass==null) {
            log.warn("no JDBC Driver class was specified by property " + Environment.DRIVER);
        }
        else {
            try {
                // trying via forName() first to be as close to DriverManager's semantics
                driver = (Driver) Class.forName(driverClass, true, Thread.currentThread().getContextClassLoader()).newInstance();
            }
            catch (Exception e) {
                try {
                    driver = (Driver) ReflectHelper.classForName(driverClass).newInstance();
                }
                catch (Exception e1) {
                    log.error(e1.getMessage());
                    throw new HibernateException(e1);
                }
            }
        }

        url = props.getProperty( Environment.URL );
        if ( url == null ) {
            String msg = "JDBC URL was not specified by property " + Environment.URL;
            log.error( msg );
            throw new HibernateException( msg );
        }

        connectionProps = ConnectionProviderFactory.getConnectionProperties( props );

        log.info( "using driver: " + driverClass + " at URL: " + url );
        // if debug level is enabled, then log the password, otherwise mask it
        if ( log.isDebugEnabled() ) {
            log.info( "connection properties: " + connectionProps );
        }
        else if ( log.isInfoEnabled() ) {
            log.info( "connection properties: " + maskOut(connectionProps, "password") );
        }

    }

    public Connection getConnection() throws SQLException {

        if ( log.isTraceEnabled() ) log.trace( "total checked-out connections: " + checkedOut );

        synchronized (pool) {
            if ( !pool.isEmpty() ) {
                int last = pool.size() - 1;
                if ( log.isTraceEnabled() ) {
                    log.trace("using pooled JDBC connection, pool size: " + last);
                    checkedOut++;
                }
                Connection pooled = (Connection) pool.remove(last);
                if (isolation!=null) pooled.setTransactionIsolation( isolation.intValue() );
                if ( pooled.getAutoCommit()!=autocommit ) pooled.setAutoCommit(autocommit);
                return pooled;
            }
        }

        log.debug("opening new JDBC connection");
        Connection conn = driver.connect(url, connectionProps);
        if (isolation!=null) conn.setTransactionIsolation( isolation.intValue() );
        if ( conn.getAutoCommit()!=autocommit ) conn.setAutoCommit(autocommit);

        if ( log.isDebugEnabled() ) {
            log.debug( "created connection to: " + url + ", Isolation Level: " + conn.getTransactionIsolation() );
        }
        if ( log.isTraceEnabled() ) checkedOut++;

        return conn;
    }

    public void closeConnection(Connection conn) throws SQLException {

        if ( log.isDebugEnabled() ) checkedOut--;

        synchronized (pool) {
            int currentSize = pool.size();
            if ( currentSize < poolSize ) {
                if ( log.isTraceEnabled() ) log.trace("returning connection to pool, pool size: " + (currentSize + 1) );
                pool.add(conn);
                return;
            }
        }

        log.debug("closing JDBC connection");

        conn.close();

    }

    protected void finalize() {
        close();
    }

    public void close() {

        log.info("cleaning up connection pool: " + url);

        Iterator iter = pool.iterator();
        while ( iter.hasNext() ) {
            try {
                ( (Connection) iter.next() ).close();
            }
            catch (SQLException sqle) {
                log.warn("problem closing pooled connection", sqle);
            }
        }
        pool.clear();

    }

    /**
     * @see ConnectionProvider#supportsAggressiveRelease()
     */
    public boolean supportsAggressiveRelease() {
        return false;
    }

    //////////////////////////////////////////////////////////////////////////
    // The following utility methods are copied from ConfigurationHelper.java
    //////////////////////////////////////////////////////////////////////////

    /**
     * Get the config value as a boolean (default of false)
     *
     * @param name The config setting name.
     * @param values The map of config values
     *
     * @return The value.
     */
    public static boolean getBoolean(String name, Map values) {
        return getBoolean( name, values, false );
    }

    /**
     * Get the config value as a boolean.
     *
     * @param name The config setting name.
     * @param values The map of config values
     * @param defaultValue The default value to use if not found
     *
     * @return The value.
     */
    public static boolean getBoolean(String name, Map values, boolean defaultValue) {
        Object value = values.get( name );
        if ( value == null ) {
            return defaultValue;
        }
        if ( Boolean.class.isInstance( value ) ) {
            return ( (Boolean) value ).booleanValue();
        }
        if ( String.class.isInstance( value ) ) {
            return Boolean.parseBoolean( (String) value );
        }
        throw new HibernateException(
                "Could not determine how to handle configuration value [name=" + name + ", value=" + value + "] as boolean"
        );
    }

    /**
     * Get the config value as an int
     *
     * @param name The config setting name.
     * @param values The map of config values
     * @param defaultValue The default value to use if not found
     *
     * @return The value.
     */
    public static int getInt(String name, Map values, int defaultValue) {
        Object value = values.get( name );
        if ( value == null ) {
            return defaultValue;
        }
        if ( Integer.class.isInstance( value ) ) {
            return ( (Integer) value ).intValue();
        }
        if ( String.class.isInstance( value ) ) {
            return Integer.parseInt( (String) value );
        }
        throw new HibernateException(
                "Could not determine how to handle configuration value [name=" + name +
                        ", value=" + value + "(" + value.getClass().getName() + ")] as int"
        );
    }

    /**
     * Get the config value as an {@link Integer}
     *
     * @param name The config setting name.
     * @param values The map of config values
     *
     * @return The value, or null if not found
     */
    public static Integer getInteger(String name, Map values) {
        Object value = values.get( name );
        if ( value == null ) {
            return null;
        }
        if ( Integer.class.isInstance( value ) ) {
            return (Integer) value;
        }
        if ( String.class.isInstance( value ) ) {
            return Integer.valueOf( (String) value );
        }
        throw new HibernateException(
                "Could not determine how to handle configuration value [name=" + name +
                        ", value=" + value + "(" + value.getClass().getName() + ")] as Integer"
        );
    }

    /**
     * replace a property by a starred version
     *
     * @param props properties to check
     * @param key proeprty to mask
     *
     * @return cloned and masked properties
     */
    public static Properties maskOut(Properties props, String key) {
        Properties clone = ( Properties ) props.clone();
        if ( clone.get( key ) != null ) {
            clone.setProperty( key, "****" );
        }
        return clone;
    }
}







