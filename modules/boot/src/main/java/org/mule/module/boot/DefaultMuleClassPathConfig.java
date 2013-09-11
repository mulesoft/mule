/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Constructs a default set of JAR Urls located under Mule home folder.
 */
// TODO this duplicates DefaultMuleClassPathConfig in the reboot module. See if this class can be moved to mule-core
public class DefaultMuleClassPathConfig
{
    protected static final String MULE_DIR = "/lib/mule";
    protected static final String USER_DIR = "/lib/user";
    protected static final String OPT_DIR = "/lib/opt";

    private List<URL> urls = new ArrayList<URL>();

    /**
     * Constructs a new DefaultMuleClassPathConfig.
     * @param muleHome Mule home directory
     * @param muleBase Mule base directory
     */
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
                this.addFile(userOverrideDir);
                this.addFiles(this.listJars(userOverrideDir));
            }
        }
        catch (IOException ioe)
        {
            System.out.println("Unable to check to see if there are local jars to load: " + ioe.toString());
        }

        File userDir = new File(muleHome, USER_DIR);
        this.addFile(userDir);
        this.addFiles(this.listJars(userDir));

        File muleDir = new File(muleHome, MULE_DIR);
        this.addFile(muleDir);
        this.addFiles(this.listJars(muleDir));

        File optDir = new File(muleHome, OPT_DIR);
        this.addFile(optDir);
        this.addFiles(this.listJars(optDir));
    }

    /**
     * @return A copy of 'urls'.
     */
    public List<URL> getURLs()
    {
        return new ArrayList<URL>(this.urls);
    }

    /**
     * Setter for property 'urls'.
     *
     * @param newUrls Value to set for property 'urls'.
     */
    public void addURLs(List<URL> newUrls)
    {
        if (newUrls != null && !newUrls.isEmpty())
        {
            this.urls.addAll(newUrls);
        }
    }

    /**
     * Add a URL to Mule's classpath.
     *
     * @param url folder (should end with a slash) or jar path
     */
    public void addURL(URL url)
    {
        this.urls.add(url);
    }

    public void addFiles(List<File> files)
    {
        for (File file : files)
        {
            this.addFile(file);
        }
    }

    public void addFile(File jar)
    {
        try
        {
            this.addURL(jar.getAbsoluteFile().toURI().toURL());
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
    protected List<File> listJars(File path)
    {
        File[] jars = path.listFiles(new FileFilter()
        {
            @Override
            public boolean accept(File pathname)
            {
                try
                {
                    return pathname.getCanonicalPath().endsWith(".jar");
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e.getMessage());
                }
            }
        });

        if (jars == null)
        {
            return Collections.emptyList();
        }
        else
        {
            return Arrays.<File>asList(jars);
        }
    }
}
