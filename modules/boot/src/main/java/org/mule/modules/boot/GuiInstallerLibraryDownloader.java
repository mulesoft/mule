/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
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

import org.mule.util.ClassUtils;

public class GuiInstallerLibraryDownloader
{
    private static String proxyHost = null;
    private static String proxyPort = null;
    private static String proxyUsername = null;
    private static String proxyPassword = null;
    
    public static void main(String args[]) throws Exception
    {
        File muleHome = new File(args[0].toString());

        // Build up a list of libraries from $MULE_HOME/lib/* and add them to the
        // classpath.
        DefaultMuleClassPathConfig classPath = new DefaultMuleClassPathConfig(muleHome, muleHome);
        addLibrariesToClasspath(classPath.getURLs());
                
        // One-time download to get libraries not included in the Mule distribution
        // and store them in MULE_HOME/lib/user.
        if (!ClassUtils.isClassOnPath("javax.activation.DataSource", GuiInstallerLibraryDownloader.class))
        {
            if (args.length > 1){
                proxyHost = args[1].toString();
            }
            if (args.length > 2){
                proxyPort = args[2].toString();
            }
            if (args.length > 3){
                proxyUsername = args[3].toString();
            }
            if (args.length > 4){
                proxyPassword = args[4].toString();               
            }
            LibraryDownloader downloader = new LibraryDownloader(muleHome, proxyHost, proxyPort, proxyUsername, proxyPassword);
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
    
        // get a Method ref from the normal class, but invoke on a proprietary parent
        // object,
        // as this method is usually protected in those classloaders
        Class refClass = URLClassLoader.class;
        Method methodAddUrl = refClass.getDeclaredMethod("addURL", new Class[]{URL.class});
        methodAddUrl.setAccessible(true);
        for (Iterator it = urls.iterator(); it.hasNext();)
        {
            URL url = (URL)it.next();
            methodAddUrl.invoke(sysCl, new Object[]{url});
        }
    }
}