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

    /**
     * Constructs a new DefaultMuleClassPathConfig.
     */
    public DefaultMuleClassPathConfig(File muleHome, File muleBase)
    {
        try
        {
            // if trailing slash is specified, the folder will be added (e.g. for
            // properties files)
            addURL(new URL("file://" + muleHome.getAbsolutePath() + FOLDER_USER + "/"));
            addURL(new URL("file://" + muleHome.getAbsolutePath() + FOLDER_MULE + "/"));
            addURL(new URL("file://" + muleHome.getAbsolutePath() + FOLDER_OPT + "/"));

            /**
             * Pick up any local jars, if there are any. Doing this here insures that
             * any local class that override the global classes will in fact do so.
             */
            try
            {
                if (!muleHome.getCanonicalFile().equals(muleBase.getCanonicalFile()))
                {
                    addURL(new URL("file://" + muleBase.getAbsolutePath() + FOLDER_USER + "/"));
                    File[] muleJars = listJars(muleBase, FOLDER_USER);
                    for (int i = 0; i < muleJars.length; i++)
                    {
                        File jar = muleJars[i];
                        addURL(jar.toURL());
                    }
                }
            }
            catch (IOException ioe)
            {
                System.out.println("Unable to check to see if there are local jars to load: "
                                + ioe.toString());
            }

            File[] muleJars = listJars(muleHome, FOLDER_USER);
            for (int i = 0; i < muleJars.length; i++)
            {
                File jar = muleJars[i];
                addURL(jar.toURL());
            }

            muleJars = listJars(muleHome, FOLDER_MULE);
            for (int i = 0; i < muleJars.length; i++)
            {
                File jar = muleJars[i];
                addURL(jar.toURL());
            }

            muleJars = listJars(muleHome, FOLDER_OPT);
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
     * @param muleSubfolder folder under the Mule directory to list
     * @return a list
     */
    protected File[] listJars(File muleDir, String muleSubfolder)
    {
        File path = new File(muleDir, muleSubfolder);

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
