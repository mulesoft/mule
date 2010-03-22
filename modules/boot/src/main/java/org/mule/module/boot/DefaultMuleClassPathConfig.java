/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.boot;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Constructs a default set of JAR Urls located under Mule home folder.
 */
public class DefaultMuleClassPathConfig
{
    protected static final String MULE_DIR = "/lib/mule";
    protected static final String USER_DIR = "/lib/user";
    protected static final String OPT_DIR = "/lib/opt";
    protected static final String APPS_DIR = "/apps";

    private List<URL> urls = new ArrayList<URL>();

    public DefaultMuleClassPathConfig(File muleHome, File muleBase)
    {
        /**
         * Pick up any local jars, if there are any. Doing this here insures that any
         * local class that override the global classes will in fact do so.
         */
        try
        {
            if (!muleHome.getCanonicalFile().equals(muleBase.getCanonicalFile()))
            {
                File userOverrideDir = new File(muleBase, USER_DIR);
                addFile(userOverrideDir);
                addFiles(listJars(userOverrideDir));
            }
        }
        catch (IOException ioe)
        {
            System.out.println("Unable to check to see if there are local jars to load: " + ioe.toString());
        }

        File userDir = new File(muleHome, USER_DIR);
        addFile(userDir);
        addFiles(listJars(userDir));

        File muleDir = new File(muleHome, MULE_DIR);
        addFile(muleDir);
        addFiles(listJars(muleDir));

        File optDir = new File(muleHome, OPT_DIR);
        addFile(optDir);
        addFiles(listJars(optDir));
        
        addAppsDir(muleHome);
    }

    public List<URL> getURLs()
    {
        return new ArrayList<URL>(this.urls);
    }

    protected void addURL(URL url)
    {
        this.urls.add(url);
    }

    protected void addFiles(File[] files)
    {
        if (files != null)
        {
            for (File f : files)
            {
                addFile(f);
            }
        }
    }

    public void addFile(File jar)
    {
        try
        {            
            addURL(jar.getAbsoluteFile().toURI().toURL());
        }
        catch (MalformedURLException mux)
        {
            throw new RuntimeException("Failed to construct a classpath URL", mux);
        }
    }

    /**
     * Find and if necessary filter the jars for classpath.
     *
     * @return a list of {@link File}s
     */
    protected File[] listJars(File path)
    {
        File[] jars = path.listFiles(new FileFilter()
        {
            public boolean accept(File pathname)
            {
                return isJar(pathname);
            }
        });

        return jars;
    }
    
    protected void addAppsDir(File muleHome)
    {
        File appsDir = new File(muleHome, APPS_DIR);
        if (appsDir.exists() == false)
        {
            return;
        }
        
        File[] apps = appsDir.listFiles(new FileFilter()
        {
            public boolean accept(File file)
            {
                return file.isDirectory();
            }
        });
        
        for (File app : apps)
        {
            addApp(app);
        }
    }
        
    protected void addApp(File app)
    {
        // add the app directory itself, it may contain configuration files
        addFile(app);
        
        File libDir = new File(app, "lib");
        if (libDir.exists())
        {
            addFiles(listJars(libDir));
        }
    }

    protected static boolean isJar(File file)
    {
        try
        {
            return file.getCanonicalPath().endsWith(".jar");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
