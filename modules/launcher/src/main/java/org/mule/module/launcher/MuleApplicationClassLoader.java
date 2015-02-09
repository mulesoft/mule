/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.module.launcher.application.ApplicationClassLoader;
import org.mule.module.launcher.artifact.AbstractArtifactClassLoader;
import org.mule.module.launcher.nativelib.NativeLibraryFinder;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

public class MuleApplicationClassLoader extends AbstractArtifactClassLoader implements ApplicationClassLoader
{

    /**
     * Library directory in Mule application.
     */
    public static final String PATH_LIBRARY = "lib";

    /**
     * Classes and resources directory in Mule application.
     */
    public static final String PATH_CLASSES = "classes";

    /**
     * Directory for standard mule modules and transports
     */
    public static final String PATH_MULE = "mule";

    /**
     * Sub-directory for per-application mule modules and transports
     */
    public static final String PATH_PER_APP = "per-app";

    protected static final URL[] CLASSPATH_EMPTY = new URL[0];

    private static final String DRIVER_MANAGER_CLEANER_CLASS = "DriverManagerCleaner.class";

    private String appName;

    private File appDir;
    private File classesDir;
    private File libDir;
    private NativeLibraryFinder nativeLibraryFinder;

    public MuleApplicationClassLoader(String appName, ClassLoader parentCl, NativeLibraryFinder nativeLibraryFinder)
    {
        this(appName, parentCl, Collections.<String>emptySet(), nativeLibraryFinder);
    }

    public MuleApplicationClassLoader(String appName, ClassLoader parentCl, Set<String> loaderOverrides, NativeLibraryFinder nativeLibraryFinder)
    {
        super(CLASSPATH_EMPTY, parentCl, loaderOverrides);
        this.appName = appName;
        this.nativeLibraryFinder = nativeLibraryFinder;

        try
        {
            appDir = MuleFoldersUtil.getAppFolder(appName);
            classesDir = new File(appDir, PATH_CLASSES);
            addURL(classesDir.toURI().toURL());

            libDir = new File(appDir, PATH_LIBRARY);
            addJars(appName, libDir, true);

            // Add per-app mule modules (if any)
            File libs = MuleFoldersUtil.getMuleLibFolder();
            File muleLibs = new File(libs, PATH_MULE);
            File perAppLibs = new File(muleLibs, PATH_PER_APP);
            addJars(appName, perAppLibs, false);
        }
        catch (IOException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("[%s]", appName), e);
            }
        }
    }

    /**
     * Add jars from the supplied directory to the class path
     */
    private void addJars(String appName, File dir, boolean verbose) throws MalformedURLException
    {
        if (dir.exists() && dir.canRead())
        {
            @SuppressWarnings("unchecked")
            Collection<File> jars = FileUtils.listFiles(dir, new String[]{"jar"}, false);

            if (!jars.isEmpty() && logger.isInfoEnabled())
            {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("[%s] Loading the following jars:%n", appName));
                sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                for (File jar : jars)
                {
                    sb.append(jar.toURI().toURL()).append(SystemUtils.LINE_SEPARATOR);
                }

                sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                if (verbose)
                {
                    logger.info(sb.toString());
                }
                else
                {
                    logger.debug(sb.toString());
                }
            }

            for (File jar : jars)
            {
                addURL(jar.toURI().toURL());
            }
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        return super.findClass(name);
    }

    @Override
    public URL getResource(String name)
    {
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException
    {
        return super.getResources(name);
    }

    public String getAppName()
    {
        return appName;
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]@%s", getClass().getName(),
                             appName,
                             Integer.toHexString(System.identityHashCode(this)));
    }

    @Override
    public String getArtifactName()
    {
        return getAppName();
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return this;
    }

    @Override
    protected String findLibrary(String name)
    {
        String libraryPath = super.findLibrary(name);

        libraryPath = nativeLibraryFinder.findLibrary(name, libraryPath);

        return libraryPath;
    }

    @Override
    protected String[] getLocalResourceLocations()
    {
        return new String[] {classesDir.getAbsolutePath()};
    }

    @Override
    public void dispose()
    {
        executeDBDriversCleanUp();
        super.dispose();
    }

    private void executeDBDriversCleanUp()
    {
        InputStream classStream = this.getClass().getResourceAsStream(DRIVER_MANAGER_CLEANER_CLASS);
        byte[] classBytes = IOUtils.toByteArray(classStream);
        Class clazz = this.defineClass(null, classBytes, 0, classBytes.length);
        try
        {
            ((Runnable) clazz.newInstance()).run();
        }
        catch (Exception e)
        {
            logger.warn("Could not unload DB Drivers.");
        }
    }

}
