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
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.derby.jdbc.EmbeddedDriver;

public class MuleDerbyTestUtils
{
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
    
    public static void cleanupDerbyDb(String derbySystemHome, String databaseName) throws IOException, SQLException
    {
        FileUtils.deleteTree(new File(derbySystemHome + "/" + databaseName));
    }
    
    public static void createDataBase(String databaseName) throws SQLException
    {
        EmbeddedDriver embeddedDriver = new EmbeddedDriver();
        embeddedDriver.connect("jdbc:derby:" + databaseName + ";create=true", null);
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


