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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    protected Set<String> overrides = new HashSet<String>();
    protected Set<String> blocked = new HashSet<String>();

    public MulePluginClassLoader(URL[] urls, ClassLoader parent)
    {
        this(urls, parent, Collections.<String>emptySet());
    }

    public MulePluginClassLoader(URL[] urls, ClassLoader parent, Set<String> overrides)
    {
        super(urls, parent);

        if (overrides != null && !overrides.isEmpty())
        {
            for (String override : overrides)
            {
                override = StringUtils.defaultString(override).trim();
                // 'blocked' package definitions come with a '-' prefix
                if (override.startsWith("-"))
                {
                    override = override.substring(1);
                    this.blocked.add(override);
                }
                this.overrides.add(override);

                for (String systemPackage : systemPackages)
                {
                    if (override.startsWith(systemPackage))
                    {
                        throw new IllegalArgumentException("Can't override a system package. Offending value: " + override);
                    }
                }
            }
        }
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
        boolean overrideMatch = isOverridden(name);


        if (overrideMatch)
        {
            System.out.printf("Name: %s Overridden: %s", name, overrideMatch);

            boolean blockedMatch = isBlocked(name);

            System.out.printf("Name: %s Blocked: %s", name, blockedMatch);

            if (blockedMatch)
            {
                // load this class from the child ONLY, don't attempt parent, let CNFE exception propagate
                result = findClass(name);
            }
            else
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


        }
        else
        {
            // no overrides, regular parent-first lookup
            try
            {
                result = findParentClass(name);
            }
            catch (ClassNotFoundException e)
            {
                result = findClass(name);
            }
        }

        if (resolve)
        {
            resolveClass(result);
        }

        return result;
    }

    protected boolean isOverridden(String name)
    {
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
        return overrideMatch;
    }

    protected boolean isBlocked(String name)
    {
        boolean blockedMatch = false;
        for (String b : blocked)
        {
            if (name.startsWith(b))
            {
                blockedMatch = true;
                break;
            }
        }
        return blockedMatch;
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
