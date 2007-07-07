/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.boot;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;

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
 *
 * Note: this class is intentionally kept free of any external library dependencies and
 * therefore repeats a few utility methods.
 */
public class MuleBootstrap
{
    public static final String MAIN_CLASS_MULE_SERVER = "org.mule.modules.boot.MuleServerWrapper";
    public static final String MAIN_CLASS_OSGI_FRAMEWORK = "org.mule.modules.osgi.OsgiFrameworkWrapper";

    public static final String CLI_OPTIONS[][] = {
        {"main", "true", "Main Class"},
        {"osgi", "false", "Run in an OSGi framework"},
        {"nogui", "false", "Suppress graphical console"}
    };

    public static void main( String[] args ) throws Exception
    {
        // Parse any command line options based on the list above.
        CommandLine commandLine = parseCommandLine(args);
        // Any unrecognized arguments get passed through to the next class (e.g., to Knopflerfish).
        String[] remainingArgs = commandLine.getArgs();

        String mainClassName = commandLine.getOptionValue("main");
        if (commandLine.hasOption("osgi"))
        {
            boolean startGui = !commandLine.hasOption("nogui");
            System.out.println("Starting the OSGi Framework...");
            WrapperManager.start(new OsgiFrameworkWrapper(startGui), remainingArgs);
        }
        else if (mainClassName == null || mainClassName.equals(MAIN_CLASS_MULE_SERVER))
        {
            configureClasspath();
            System.out.println("Starting the Mule Server...");
            WrapperManager.start((WrapperListener) Class.forName(MAIN_CLASS_MULE_SERVER).newInstance(), remainingArgs);
        }
        else
        {
            // Add the main class name as the first argument to the Wrapper.
            String[] appArgs = new String[remainingArgs.length + 1];
            appArgs[0] = mainClassName;
            System.arraycopy(remainingArgs, 0, appArgs, 1, remainingArgs.length);
            configureClasspath();
            System.out.println("Starting class " + mainClassName + "...");
            WrapperSimpleApp.main(appArgs);
        }
    }

    private static void configureClasspath() throws Exception
    {
        // Make sure MULE_HOME is set.
        File muleHome = null;

        String muleHomeVar = System.getProperty("mule.home");
        // Note: we can't use StringUtils.isBlank() here because we don't have that
        // library yet.
        if (muleHomeVar != null && !muleHomeVar.trim().equals("") && !muleHomeVar.equals("%MULE_HOME%"))
        {
            muleHome = new File(muleHomeVar).getCanonicalFile();
        }
        if (muleHome == null || !muleHome.exists() || !muleHome.isDirectory())
        {
            throw new IllegalArgumentException(
                "Either MULE_HOME is not set or does not contain a valid directory.");
        }

        File muleBase;

        String muleBaseVar = System.getProperty("mule.base");
        if (muleBaseVar != null && !muleBaseVar.trim().equals("") && !muleBaseVar.equals("%MULE_BASE%"))
        {
            muleBase = new File(muleBaseVar).getCanonicalFile();
        }
        else
        {
            muleBase = muleHome;
        }

        // Build up a list of libraries from $MULE_HOME/lib/* and add them to the
        // classpath.
        DefaultMuleClassPathConfig classPath = new DefaultMuleClassPathConfig(muleHome, muleBase);
        addLibrariesToClasspath(classPath.getURLs());

        // If the license ack file isn't on the classpath, we need to
        // display the EULA and make sure the user accepts it before continuing
        if (ReflectionHelper.getResource("META-INF/mule/license.props", MuleBootstrap.class) == null)
        {
            LicenseHandler licenseHandler = new LicenseHandler(muleHome, muleBase);
            // If the user didn't accept the license, then we have to exit
            // Exiting this way insures that the wrapper won't try again
            // (by default it'll try to start 3 times)
            if (!licenseHandler.getAcceptance())
            {
                ReflectionHelper.wrapperStop(-1);
            }
        }

        // One-time download to get libraries not included in the Mule distribution
        // due to silly licensing restrictions.
        //
        // Now we will download these libraries to MULE_BASE/lib/user. In
        // a standard installation, MULE_BASE will be MULE_HOME.
        if (!ReflectionHelper.isClassOnPath("javax.activation.DataSource", MuleBootstrap.class))
        {
            LibraryDownloader downloader = new LibraryDownloader(muleBase);
            addLibrariesToClasspath(downloader.downloadLibraries());
        }
    }

    private static void addLibrariesToClasspath(List urls)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {

        ClassLoader sys = ClassLoader.getSystemClassLoader();
        if (!(sys instanceof URLClassLoader))
        {
            throw new IllegalArgumentException(
                "PANIC: Mule has been started with an unsupported classloader: " + sys.getClass().getName()
                                + ". " + "Please report this error to user<at>mule<dot>codehaus<dot>org");
        }

        // system classloader is in this case the one that launched the application,
        // which is usually something like a JDK-vendor proprietary AppClassLoader
        URLClassLoader sysCl = (URLClassLoader)sys;

        /*
         * IMPORTANT NOTE: The more 'natural' way would be to create a custom
         * URLClassLoader and configure it, but then there's a chicken-and-egg
         * problem, as all classes MuleBootstrap depends on would have been loaded by
         * a parent classloader, and not ours. There's no straightforward way to
         * change this, and is documented in a Sun's classloader guide. The solution
         * would've involved overriding the ClassLoader.findClass() method and
         * modifying the semantics to be child-first, but that way we are calling for
         * trouble. Hacking the primordial classloader is a bit brutal, but works
         * perfectly in case of running from the command-line as a standalone app.
         * All Mule embedding options then delegate the classpath config to the
         * embedder (a developer embedding Mule in the app), thus classloaders are
         * not modified in those scenarios.
         */

        // get a Method ref from the normal class, but invoke on a proprietary parent
        // object,
        // as this method is usually protected in those classloaders
        Class refClass = URLClassLoader.class;
        Method methodAddUrl = refClass.getDeclaredMethod("addURL", new Class[]{URL.class});
        methodAddUrl.setAccessible(true);
        for (Iterator it = urls.iterator(); it.hasNext();)
        {
            URL url = (URL)it.next();
            // System.out.println("Adding: " + url.toExternalForm());
            methodAddUrl.invoke(sysCl, new Object[]{url});
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
            options.addOption(CLI_OPTIONS[i][0], CLI_OPTIONS[i][1].equals("true") ? true : false, CLI_OPTIONS[i][2]);
        }
        return new BasicParser().parse(options, args, true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The following utility methods are included here in order to keep the bootloader
    // free of any external library dependencies.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Imitates ClassUtils.isClassOnPath()
     */
    private static boolean isClassOnPath(String className) {
        boolean found = false;
        try {
            found = (Thread.currentThread().getContextClassLoader().loadClass(className) != null);
        } catch (ClassNotFoundException e) { }
        if (!found) {
            try {
                found = (Class.forName(className) != null);
            } catch (ClassNotFoundException e) { }
        }
        if (!found) {
            try {
                found = (MuleBootstrap.class.getClassLoader().loadClass(className) != null);
            } catch (ClassNotFoundException e) { }
        }
        return found;
    }
}
