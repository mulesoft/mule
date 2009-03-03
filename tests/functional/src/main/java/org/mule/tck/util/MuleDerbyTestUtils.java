/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.util;

import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * NOTE: Don't forget to duplicate all the changes in {@link org.mule.example.loanbroker.bpm.DbUtils}
 */
public class MuleDerbyTestUtils
{
    private static final String DERBY_DRIVER_CLASS = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String DERBY_DATASOURCE_CLASS = "org.apache.derby.jdbc.EmbeddedDataSource";
    
    //class cannot be instantiated
    private MuleDerbyTestUtils()
    {
        super();
    }
    
    //by default, set the derby home to the target directory
    public static String setDerbyHome()
    {
        return setDerbyHome("target");
    }
    
    public static String setDerbyHome(String path)
    {
        File derbySystemHome = new File(System.getProperty("user.dir"), path);
        System.setProperty("derby.system.home",  derbySystemHome.getAbsolutePath());
        return derbySystemHome.getAbsolutePath();
    }
    
    /**
     * Properly shutdown an embedded Derby database
     * 
     * @throws SQLException
     * @see <h href="http://db.apache.org/derby/docs/10.3/devguide/tdevdvlp20349.html">Derby docs</a>
     */
    public static void stopDatabase() throws SQLException
    {
        try
        {
            // force loading the driver so it's available even if no prior connection to the
            // database was made
            ClassUtils.instanciateClass(DERBY_DRIVER_CLASS);

            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        }
        catch (SQLException sqlex)
        {
            // this exception is documented to be thrown upon shutdown
            if (!"XJ015".equals(sqlex.getSQLState()))
            {
                throw sqlex;
            }
        }
        catch (Exception ex)
        {
            // this can only happen when the driver class is not in classpath. In this case, just
            // throw up
            throw new RuntimeException(ex);
        }
    }
    
    public static void cleanupDerbyDb(String databaseName) throws IOException, SQLException
    {
        cleanupDerbyDb(setDerbyHome(), databaseName);
    }
    
    public static void cleanupDerbyDb(String derbySystemHome, String databaseName) throws IOException, SQLException
    {
        stopDatabase();
        FileUtils.deleteTree(new File(derbySystemHome + File.separator + databaseName));
    }
    
    /** Start a previously created (and stopped) database */
    public static void startDataBase(String databaseName) throws Exception
    {
        Driver derbyDriver = (Driver) ClassUtils.instanciateClass(DERBY_DRIVER_CLASS);

        Method connectMethod = derbyDriver.getClass().getMethod("connect", String.class, Properties.class);

        String connectionName = "jdbc:derby:" + databaseName;
        connectMethod.invoke(derbyDriver, connectionName, null);
    }

    /**
     * Create a new embedded database
     * @param databaseName
     * @throws SQLException
     */
    public static void createDataBase(String databaseName) throws SQLException
    {
        createDataBase(databaseName, (String[]) null);
    }

    /**
     * Create a new embedded database
     * @param databaseName
     * @param creationSql - SQL used to create and populate initial database tables
     * @throws SQLException
     */
    public static void createDataBase(String databaseName, String creationSql) throws SQLException
    {
        createDataBase(databaseName, new String[] { creationSql } );
    }
    
    /**
     * Create a new embedded database
     * @param databaseName
     * @param creationSql - SQL used to create and populate initial database tables
     * @throws SQLException
     */
    public static void createDataBase(String databaseName, String[] creationSql) throws SQLException
    {
        createDataBase(databaseName, creationSql, null);
    }
    
    /**
     * Create a new embedded database
     * @param databaseName
     * @param creationSql - SQL used to create and populate initial database tables
     * @throws SQLException
     */
    public static void createDataBase(String databaseName, String[] creationSql, Properties properties) throws SQLException
    {
        // Do not use the EmbeddedDriver class here directly to avoid compile time references
        // on derby.jar
        try
        {
            String connectionName = "jdbc:derby:" + databaseName + ";create=true";
            /*
             * EmbeddedDriver derbyDriver = new EmbeddedDriver();
             * derbyDriver.connect(connectionName, null);
             */
            Driver derbyDriver = (Driver) ClassUtils.instanciateClass(DERBY_DRIVER_CLASS);
            Method connectMethod = derbyDriver.getClass().getMethod("connect", String.class, Properties.class);
            connectMethod.invoke(derbyDriver, connectionName, properties);

            if (creationSql != null)
            {
                /*
                 * EmbeddedDataSource embeddedDS = new EmbeddedDataSource();
                 * embeddedDS.setDatabaseName(databaseName);
                 */
                DataSource embeddedDS = (DataSource) ClassUtils.instanciateClass(DERBY_DATASOURCE_CLASS);
                Method m = embeddedDS.getClass().getMethod("setDatabaseName", String.class);
                m.invoke(embeddedDS, databaseName);

                Connection con = null;
                try
                {
                    con = embeddedDS.getConnection();
                    Statement st = con.createStatement();
                    for (String aCreationSql : creationSql)
                    {
                        st.execute(aCreationSql);
                    }
                    con.commit();
                }
                finally
                {
                    if (con != null && !con.isClosed())
                    {
                        con.close();
                    }
                }
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Error creating the database " + databaseName, ex);
        }
    }
    
    public static String loadDatabaseName(String propertiesLocation, String propertyName) throws IOException
    {
        Properties derbyProperties = new Properties();
        URL resource = ClassUtils.getResource(propertiesLocation, MuleDerbyTestUtils.class);
        derbyProperties.load(resource.openStream());
        return derbyProperties.getProperty(propertyName);
    }

    public static void defaultDerbyCleanAndInit(String propertiesLocation, String propertyName) throws IOException, SQLException
    {
        String derbyHome = setDerbyHome();
        String dbName = loadDatabaseName(propertiesLocation, propertyName);
        cleanupDerbyDb(derbyHome, dbName);
        createDataBase(dbName);
    }
}


