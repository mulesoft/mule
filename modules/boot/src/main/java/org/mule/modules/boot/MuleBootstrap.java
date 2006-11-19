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

import org.mule.MuleServer;
import org.mule.util.ClassUtils;
import org.mule.util.SystemUtils;
import org.tanukisoftware.wrapper.WrapperSimpleApp;

/**
 * Determine which is the main class to run and delegate control to the Java Service
 * Wrapper. <p/> MuleBootstrap class is responsible for constructing Mule's classpath
 * from the Mule home folder.
 */
public class MuleBootstrap
{

    /**
     * Do not instantiate MuleBootstrap.
     */
    private MuleBootstrap()
    {
        super();
    }

    /**
     * Entry point.
     *
     * @param args command-line arguments
     * @throws Exception in case of any fatal problem
     */
    public static void main(String args[]) throws Exception
    {
        // Make sure MULE_HOME is set.
        File muleHome = null;
        String muleHomeVar = System.getProperty("mule.home");
        // Note: we can't use StringUtils.isBlank() here because we don't have that library yet.
        if (muleHomeVar != null && !muleHomeVar.trim().equals("")) {
            muleHome = new File(muleHomeVar).getCanonicalFile();
        }
        if (muleHome == null || !muleHome.exists() || !muleHome.isDirectory()) {
            throw new IllegalArgumentException(
                "Either MULE_HOME is not set or does not contain a valid directory.");
        }

        // Build up a list of libraries from $MULE_HOME/lib/* and add them to the classpath.
        DefaultMuleClassPathConfig classPath = new DefaultMuleClassPathConfig(muleHome);
        addLibrariesToClasspath(classPath.getURLs());

        // One-time download to get libraries not included in the Mule distribution due
        // to silly licensing restrictions.
        if (!ClassUtils.isClassOnPath("javax.activation.DataSource", MuleBootstrap.class)) {
            LibraryDownloader downloader = new LibraryDownloader(muleHome);
            addLibrariesToClasspath(downloader.downloadLibraries());
        }

        // the core jar has been added dynamically, this construct will run with
        // a new Mule classpath now
        String mainClassName = SystemUtils.getCommandLineOption("-main", args);

        if (mainClassName == null)
        {
            mainClassName = MuleServer.class.getName();
        }

        // Add the main class name as the first argument to the Wrapper.
        String[] appArgs = new String[args.length + 1];
        appArgs[0] = mainClassName;
        System.arraycopy(args, 0, appArgs, 1, args.length);

        // Call the wrapper
        WrapperSimpleApp.main(appArgs);
    }

    private static void addLibrariesToClasspath(List urls)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

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
}
