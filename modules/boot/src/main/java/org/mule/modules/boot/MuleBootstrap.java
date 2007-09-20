/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.boot;

import org.mule.util.ClassUtils;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.tanukisoftware.wrapper.WrapperListener;
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
    private static final String MULE_MODULE_BOOT_POM_FILE_PATH = "META-INF/maven/org.mule.modules/mule-module-boot/pom.properties";

    public static final String MAIN_CLASS_MULE_SERVER = "org.mule.modules.boot.MuleServerWrapper";
    public static final String MAIN_CLASS_OSGI_FRAMEWORK = "org.mule.modules.osgi.OsgiFrameworkWrapper";

    public static final String CLI_OPTIONS[][] = {
            {"main", "true", "Main Class"},
            {"osgi", "false", "Run in an OSGi framework"},
            {"nogui", "false", "Suppress graphical console"},
            {"version", "false", "Show product and version information"}
    };

    public static void main(String[] args) throws Exception
    {
        // Parse any command line options based on the list above.
        CommandLine commandLine = parseCommandLine(args);
        // Any unrecognized arguments get passed through to the next class (e.g., to Knopflerfish).
        String[] remainingArgs = commandLine.getArgs();

        String mainClassName = commandLine.getOptionValue("main");
        if (commandLine.hasOption("version"))
        {
            prepareBootstrapPhase();
            WrapperManager.start(new VersionWrapper(), remainingArgs);
        }
        else if (commandLine.hasOption("osgi"))
        {
            boolean startGui = !commandLine.hasOption("nogui");
            System.out.println("Starting the OSGi Framework...");
            WrapperManager.start(new OsgiFrameworkWrapper(startGui), remainingArgs);
        }
        else if (mainClassName == null || mainClassName.equals(MAIN_CLASS_MULE_SERVER))
        {
            prepareBootstrapPhase();
            System.out.println("Starting the Mule Server...");
            WrapperManager.start((WrapperListener) Class.forName(MAIN_CLASS_MULE_SERVER).newInstance(), remainingArgs);
        }
        else
        {
            // Add the main class name as the first argument to the Wrapper.
            String[] appArgs = new String[remainingArgs.length + 1];
            appArgs[0] = mainClassName;
            System.arraycopy(remainingArgs, 0, appArgs, 1, remainingArgs.length);
            prepareBootstrapPhase();
            System.out.println("Starting class " + mainClassName + "...");
            WrapperSimpleApp.main(appArgs);
        }
    }
    
    private static void prepareBootstrapPhase() throws Exception
    {
        File muleHome = lookupMuleHome();
        File muleBase = lookupMuleBase();

        if (muleBase == null)
        {
            muleBase = muleHome;
        }

        MuleBootstrapUtils.addLocalJarFilesToClasspath(muleHome, muleBase);
        MuleBootstrapUtils.addExternalJarFilesToClasspath(muleHome, null);
        
        setSystemMuleVersion();
        requestLicenseAcceptance();        
    }
    
    private static File lookupMuleHome() throws Exception
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
    
    private static File lookupMuleBase() throws Exception
    {
        File muleBase = null;
        String muleBaseVar = System.getProperty("mule.base");
        
        if (muleBaseVar != null && !muleBaseVar.trim().equals("") && !muleBaseVar.equals("%MULE_BASE%"))
        {
            muleBase = new File(muleBaseVar).getCanonicalFile();
        }
        return muleBase;
    }    
    
    private static void requestLicenseAcceptance() throws Exception
    {
        if (!LicenseHandler.isLicenseAccepted() && !LicenseHandler.getAcceptance())
        {
            WrapperManager.stop(-1);
        }        
    }
    
    private static void setSystemMuleVersion()
    {
        try
        {
            URL mavenPropertiesUrl = ClassUtils.getResource(MULE_MODULE_BOOT_POM_FILE_PATH, MuleServerWrapper.class);
            Properties mavenProperties = new Properties();
            mavenProperties.load(mavenPropertiesUrl.openStream());
            
            System.setProperty("mule.version", mavenProperties.getProperty("version"));
            System.setProperty("mule.reference.version", mavenProperties.getProperty("version") + '-' + (new Date()).getTime());
        }
        catch (Exception ignore)
        {
            // ignore;
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
