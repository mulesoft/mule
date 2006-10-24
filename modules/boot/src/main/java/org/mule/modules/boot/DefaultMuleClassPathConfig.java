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
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Constructs a default set of JAR Urls located under Mule home folder.
 */
public class DefaultMuleClassPathConfig
{
    protected static final String FOLDER_MULE = "/lib/mule";
    protected static final String FOLDER_OPT = "/lib/opt";
    protected static final String FOLDER_USER = "/lib/user";

    private List urls = new LinkedList();

    private String muleHome;

    /**
     * Constructs a new DefaultMuleClassPathConfig.
     */
    public DefaultMuleClassPathConfig()
    {
        muleHome = System.getProperty("mule.home");
        if (muleHome == null || muleHome.trim().length() == 0)
        {
            throw new IllegalArgumentException(
                "Either MULE_HOME is not set or mule.home system property is missing.");
        }

        try
        {
            // if trailing slash is specified, the folder will be added (e.g. for
            // properties files)
            addURL(new URL("file:///" + muleHome + FOLDER_USER + "/"));
            addURL(new URL("file:///" + muleHome + FOLDER_MULE + "/"));
            addURL(new URL("file:///" + muleHome + FOLDER_OPT + "/"));

            File[] muleJars = listJars(FOLDER_USER);
            for (int i = 0; i < muleJars.length; i++)
            {
                File jar = muleJars[i];
                addURL(jar.toURL());
            }

            muleJars = listJars(FOLDER_MULE);
            for (int i = 0; i < muleJars.length; i++)
            {
                File jar = muleJars[i];
                addURL(jar.toURL());
            }

            muleJars = listJars(FOLDER_OPT);
            for (int i = 0; i < muleJars.length; i++)
            {
                File jar = muleJars[i];
                addURL(jar.toURL());
            }

        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException("Failed to construct a classpath URL", e);
        }

    }

    /**
     * Getter for property 'urls'.
     * 
     * @return A copy of 'urls'. Items are java.net.URL
     */
    public List getURLs()
    {
        return new ArrayList(this.urls);
    }

    /**
     * Setter for property 'urls'.
     * 
     * @param urls Value to set for property 'urls'.
     */
    public void addURLs(List urls)
    {
        if (urls != null && !urls.isEmpty())
        {
            this.urls.addAll(urls);
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

    /**
     * Find and if necessary filter the jars for classpath.
     * 
     * @param muleSubfolder folder under Mule home to list
     * @return a list
     */
    protected File[] listJars(String muleSubfolder)
    {
        String fullPath = muleHome + muleSubfolder;
        File path = new File(fullPath);

        File[] jars = path.listFiles(new FileFilter()
        {
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

        return jars == null ? new File[0] : jars;
    }
}
