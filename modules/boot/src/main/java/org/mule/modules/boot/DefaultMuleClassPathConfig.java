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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Constructs a default set of JAR Urls located under Mule home folder.
 */
public class DefaultMuleClassPathConfig
{
    protected static final String MULE_DIR = "/lib/mule";
    protected static final String USER_DIR = "/lib/user";
    protected static final String OPT_DIR = "/lib/opt";

    private List urls = new ArrayList();

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

    public void addFiles(List files)
    {
        for (Iterator i = files.iterator(); i.hasNext();)
        {
            this.addFile((File)i.next());
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
    protected List listJars(File path)
    {
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

        return jars == null ? Collections.EMPTY_LIST : Arrays.asList(jars);
    }

}
