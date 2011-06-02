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

import java.net.URL;

/**
 * Handles Mule extensions in $MULE_HOME/lib/ext
 */
public class MulePluginClassLoader extends GoodCitizenClassLoader
{

    protected String appName;

    // TODO delete this switch
    protected boolean parentFirst = true;

    // TODO double-check, we might need to restrict logging libs loading too
    protected String[] systemPackages = {
            "java",
            "javax",
            "org.mule",
            "com.mulesoft",
            "com.mulesource"
    };

    protected String[] overrides = {

    };

    public MulePluginClassLoader(URL[] urls, ClassLoader parent)
    {
        this(urls, parent, null);
    }

    /**
     * @param appName when specified, temp files will be stored under this app's working dir;
     *                if null, a top-level work dir is used
     */
    public MulePluginClassLoader(URL[] urls, ClassLoader parent, String appName)
    {
        super(urls, parent);

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

    }

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

    // TODO remove this method
    public static void main(String[] args) throws Exception
    {
        System.setProperty("mule.home", "c:\\java\\mule\\mule-standalone-3.2.0-SNAPSHOT");
        MulePluginClassLoader ext = new MulePluginClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
        Class c = ext.loadClass("junit.framework.Assert");
        URL location = c.getProtectionDomain().getCodeSource().getLocation();
        System.out.println("\nparent first");
        System.out.println("location = " + location);

        // reset CL
        ext = new MulePluginClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
        ext.parentFirst = false;
        System.out.println("\nchild first");
        c = ext.loadClass("junit.framework.Assert");
        location = c.getProtectionDomain().getCodeSource().getLocation();
        System.out.println("location = " + location);

        // reset CL & configure overrides
        ext = new MulePluginClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
        System.out.println("\noverrides");
        ext.overrides = new String[] {"junit"};
        c = ext.loadClass("junit.framework.Assert");
        location = c.getProtectionDomain().getCodeSource().getLocation();

        System.out.println("location = " + location);

    }
}
