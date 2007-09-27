/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;


public class MuleDerbyTestUtils
{
    private static final String DERBY_DRIVER_CLASS = "org.apache.derby.jdbc.EmbeddedDriver";
    
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
            ClassUtils.instanciateClass(DERBY_DRIVER_CLASS, new Object[0]);

            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        }
        catch (SQLException sqlex)
        {
            // this exception is documented to be thrown upon shutdown
            if (sqlex.getSQLState().equals("XJ015") == false)
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
    
    public static void cleanupDerbyDb(String derbySystemHome, String databaseName) throws IOException, SQLException
    {
        stopDatabase();
        FileUtils.deleteTree(new File(derbySystemHome + File.separator + databaseName));
    }
    
    public static void createDataBase(String databaseName) throws SQLException
    {
        // Do not use the EmbeddedDriver class here directly to avoid compile time references
        // on derby.jar
        try
        {
            Driver derbyDriver = (Driver) ClassUtils.instanciateClass(DERBY_DRIVER_CLASS, new Object[0]);
            
            Method connectMethod = derbyDriver.getClass().getMethod("connect", 
                new Class[] { String.class, Properties.class });
            
            String connectionName = "jdbc:derby:" + databaseName + ";create=true";
            connectMethod.invoke(derbyDriver, new Object[] { connectionName, null });
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Error creating the database " + databaseName, ex);
        }
    }
    
    public static String loadDatabaseName(InputStream propertiesStream, String propertyName) throws IOException
    {
        Properties derbyProperties = new Properties();
        derbyProperties.load(propertiesStream);
        return derbyProperties.getProperty(propertyName);
    }
    
    public static void defaultDerbyCleanAndInit(InputStream propertiesStream, String propertyName) throws IOException, SQLException
    {
        String derbyHome = setDerbyHome();
        String dbName = loadDatabaseName(propertiesStream, propertyName);
        cleanupDerbyDb(derbyHome, dbName);
        createDataBase(dbName);
    }
}


