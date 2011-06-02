/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.plugin;

import org.mule.module.launcher.GoodCitizenClassLoader;
import org.mule.util.StringUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MulePluginClassLoader extends GoodCitizenClassLoader
{

    protected String appName;

    protected String[] systemPackages = {
            "java",
            "javax",
            "org.mule",
            "com.mulesoft",
            "com.mulesource"
    };

    protected List<String> overrides = new ArrayList<String>();
    protected List<String> blocked = new ArrayList<String>();

    public MulePluginClassLoader(URL[] urls, ClassLoader parent)
    {
        this(urls, parent, Collections.<String>emptyList());
    }

    public MulePluginClassLoader(URL[] urls, ClassLoader parent, List<String> overrides)
    {
        super(urls, parent);

        if (overrides != null && !overrides.isEmpty())
        {
            for (String override : overrides)
            {
                for (String systemPackage : systemPackages)
                {
                    override = StringUtils.defaultString(override).trim();
                    // 'blocked' package definitions come with a '-' prefix
                    if (override.startsWith("-"))
                    {
                        override = override.substring(1);
                        this.blocked.add(override);
                    }
                    if (override.startsWith(systemPackage))
                    {
                        throw new IllegalArgumentException("Can't override a system package. Offending value: " + override);
                    }
                    this.overrides.add(override);
                }
            }
        }

        this.overrides = overrides;
    }

    ///**
    // * @param appName when specified, temp files will be stored under this app's working dir;
    // *                if null, a top-level work dir is used
    // */
    //public MulePluginClassLoader(URL[] urls, ClassLoader parent, String appName)
    //{
    //    super(urls, parent);

        /*String s = "c:\\java\\mule\\mule-standalone-3.2.0-SNAPSHOT\\lib\\user\\ext-test.zip";

        try
        {
            final long now = System.currentTimeMillis();
            JarFile zip = new JarFile(s);
            final Enumeration<? extends JarEntry> entries = zip.entries();
            while (entries.hasMoreElements())
            {
                JarEntry zipEntry = entries.nextElement();
                if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".jar"))
                {
                    // TODO we are unpacking on each deployment to avoid jar clashes, see if there's a better way maybe
                    // resolve app's tmp dir
                    final File tmpDir = new File(MuleContainerBootstrapUtils.getMuleTmpDir(),
                                                 StringUtils.defaultString(appName) + now);
                    tmpDir.mkdirs();
                    File outJar = new File(tmpDir, zipEntry.getName());
                    // extract jars
                    IOUtils.copy(zip.getInputStream(zipEntry), new FileOutputStream(outJar));
                    addURL(outJar.toURI().toURL());
                }
            }
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }*/

    //}

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        //System.out.println("MulePluginClassLoader.loadClass " + name);

        Class<?> result = findLoadedClass(name);

        if (result != null)
        {
            return result;
        }

        // find a match
        boolean overrideMatch = false;
        for (String override : overrides)
        {
            if (name.startsWith(override))
            {
                overrideMatch = true;
                break;
            }
        }

        if (overrideMatch)
        {
            // load this class from the child
            try
            {
                result = findClass(name);
            }
            catch (ClassNotFoundException e)
            {
                // let it fail with CNFE
                result = findParentClass(name);
            }
        }
        else
        {
            try
            {
                result = findParentClass(name);
            }
            catch (ClassNotFoundException e)
            {
                result = findClass(name);
                if (resolve)
                {
                    resolveClass(result);
                }
            }
        }

        return result;
    }

    protected Class<?> findParentClass(String name) throws ClassNotFoundException
    {
        if (getParent() != null)
        {
            return getParent().loadClass(name);
        }
        else
        {
            return findSystemClass(name);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        //System.out.println("MulePluginClassLoader.findClass " + name);
        return super.findClass(name);
    }

}
