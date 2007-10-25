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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;

public final class MuleBootstrapUtils
{
    private static final String MULE_LIB_FILENAME = "lib" + File.separator + "mule";
    private static final String MULE_HOME = System.getProperty("mule.home");
    
    public static final String MULE_LOCAL_JAR_FILENAME = "mule-local-install.jar";

    private MuleBootstrapUtils()
    {
        // utility class only
    }
    
    public static File getMuleHomeFile()
    {
        return new File(MULE_HOME);
    }
    
    public static File getMuleLibDir()
    {   
        return new File(MULE_HOME + File.separator + MULE_LIB_FILENAME);
    }
    
    public static File getMuleLocalJarFile()
    {
        return new File(getMuleLibDir(), MULE_LOCAL_JAR_FILENAME);
    }
    
    public static void addLocalJarFilesToClasspath(File muleHome, File muleBase) throws Exception
    {
        DefaultMuleClassPathConfig classPath = new DefaultMuleClassPathConfig(muleHome, muleBase);
        addLibrariesToClasspath(classPath.getURLs());
    }    
    
    public static void addExternalJarFilesToClasspath(File muleHome, ProxyInfo proxyInfo) throws Exception
    {
        LibraryDownloader downloader = null;
        if (proxyInfo != null)
        {
            downloader = new LibraryDownloader(muleHome, proxyInfo.host, proxyInfo.port, proxyInfo.username, proxyInfo.password);
        }
        else 
        {
            downloader = new LibraryDownloader(muleHome);
        }
        addLibrariesToClasspath(downloader.downloadLibraries());
    }
    
    public static void addLibrariesToClasspath(List urls) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
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
        URLClassLoader sysCl = (URLClassLoader) sys;
    
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
            URL url = (URL) it.next();
            methodAddUrl.invoke(sysCl, new Object[]{url});
        }
    }
    
    public static class ProxyInfo
    {
        String host;
        String port;
        String username;
        String password;
    }
}
