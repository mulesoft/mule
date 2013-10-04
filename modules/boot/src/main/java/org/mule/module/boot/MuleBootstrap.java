/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.boot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.tanukisoftware.wrapper.WrapperManager;
import org.tanukisoftware.wrapper.WrapperSimpleApp;

/**
 * Determine which is the main class to run and delegate control to the Java Service
 * Wrapper.  If OSGi is not being used to boot with, configure the classpath based on
 * the libraries in $MULE_HOME/lib/*
 * <p/>
 * Note: this class is intentionally kept free of any external library dependencies and
 * therefore repeats a few utility methods.
 */
public class MuleBootstrap
{
    private static final String MULE_MODULE_BOOT_POM_FILE_PATH = "META-INF/maven/org.mule.module/mule-module-boot/pom.properties";
 
    public static final String CLI_OPTIONS[][] = {
        {"main", "true", "Main Class"},
        {"production", "false", "Modify the system class loader for production use (as in Mule 2.x)"},
        {"version", "false", "Show product and version information"}
    };

    public static void main(String[] args) throws Exception
    {
        // Parse any command line options based on the list above.
        CommandLine commandLine = parseCommandLine(args);
        // Any unrecognized arguments get passed through to the next class (e.g., to the OSGi Framework).
        String[] remainingArgs = commandLine.getArgs();

        prepareBootstrapPhase(commandLine);
        
        String mainClassName = commandLine.getOptionValue("main");
        if (commandLine.hasOption("version"))
        {
            WrapperManager.start(new VersionWrapper(), remainingArgs);
        }
        else if (mainClassName == null || mainClassName.equals(MuleServerWrapper.class.getName()))
        {
            System.out.println("Starting the Mule Server...");
            WrapperManager.start(new MuleServerWrapper(), remainingArgs);
        }
        else
        {
            // Add the main class name as the first argument to the Wrapper.
            String[] appArgs = new String[remainingArgs.length + 1];
            appArgs[0] = mainClassName;
            System.arraycopy(remainingArgs, 0, appArgs, 1, remainingArgs.length);
            System.out.println("Starting class " + mainClassName + "...");
            WrapperSimpleApp.main(appArgs);
        }
    }

    private static void prepareBootstrapPhase(CommandLine commandLine) throws Exception
    {
        boolean production = commandLine.hasOption("production");                
        prepareBootstrapPhase(production);
    }
    
    private static void prepareBootstrapPhase(boolean production) throws Exception
    {
        File muleHome = lookupMuleHome();
        File muleBase = lookupMuleBase();
        if (muleBase == null)
        {
            muleBase = muleHome;
        }

        if (production)
        {            
            MuleBootstrapUtils.addLocalJarFilesToClasspath(muleHome, muleBase);
        }
        
        setSystemMuleVersion();
    }
    
    public static File lookupMuleHome() throws Exception
    {
        File muleHome = null;
        String muleHomeVar = System.getProperty("mule.home");
        
        if (muleHomeVar != null && !muleHomeVar.trim().equals("") && !muleHomeVar.equals("%MULE_HOME%"))
        {
            muleHome = new File(muleHomeVar).getCanonicalFile();
        }

        if (muleHome == null || !muleHome.exists() || !muleHome.isDirectory())
        {
            throw new IllegalArgumentException("Either MULE_HOME is not set or does not contain a valid directory.");
        }
        return muleHome;
    }
    
    public static File lookupMuleBase() throws Exception
    {
        File muleBase = null;
        String muleBaseVar = System.getProperty("mule.base");
        
        if (muleBaseVar != null && !muleBaseVar.trim().equals("") && !muleBaseVar.equals("%MULE_BASE%"))
        {
            muleBase = new File(muleBaseVar).getCanonicalFile();
        }
        return muleBase;
    }

    private static void setSystemMuleVersion()
    {
        InputStream propertiesStream = null;
        try
        {
            URL mavenPropertiesUrl = MuleBootstrapUtils.getResource(MULE_MODULE_BOOT_POM_FILE_PATH, MuleServerWrapper.class);
            propertiesStream = mavenPropertiesUrl.openStream();
            
            Properties mavenProperties = new Properties();
            mavenProperties.load(propertiesStream);
            
            System.setProperty("mule.version", mavenProperties.getProperty("version"));
            System.setProperty("mule.reference.version", mavenProperties.getProperty("version") + '-' + (new Date()).getTime());
        }
        catch (Exception ignore)
        {
            // ignore;
        }
        finally
        {
            if (propertiesStream != null)
            {
                try
                {
                    propertiesStream.close();
                }
                catch (IOException iox)
                {
                    // ignore
                }
            }
        }
    }

    /**
     * Parse any command line arguments using the Commons CLI library.
     */
    private static CommandLine parseCommandLine(String[] args) throws ParseException
    {
        Options options = new Options();
        for (int i = 0; i < CLI_OPTIONS.length; i++)
        {
            options.addOption(CLI_OPTIONS[i][0], "true".equalsIgnoreCase(CLI_OPTIONS[i][1]), CLI_OPTIONS[i][2]);
        }
        return new BasicParser().parse(options, args, true);
    }
}
