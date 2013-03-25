/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import org.mule.api.config.MuleProperties;
import org.mule.module.launcher.application.ApplicationClassLoader;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MuleApplicationClassLoader extends FineGrainedControlClassLoader implements ApplicationClassLoader
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
    protected final transient Log logger = LogFactory.getLog(getClass());

    protected List<ShutdownListener> shutdownListeners = new ArrayList<ShutdownListener>();

    private String appName;

    private String libraryPath;

    public MuleApplicationClassLoader(String appName, ClassLoader parentCl)
    {
        this(appName, parentCl, Collections.<String>emptySet());
    }

    public MuleApplicationClassLoader(String appName, ClassLoader parentCl, Set<String> loaderOverrides)
    {
        super(CLASSPATH_EMPTY, parentCl, loaderOverrides);
        this.appName = appName;
        try
        {
            // get lib dir
            final String muleHome = System.getProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
            String configPath = String.format("%s/apps/%s", muleHome, appName);
            File parentFile = new File(configPath); 
            File classesDir = new File(parentFile, PATH_CLASSES);
            addURL(classesDir.toURI().toURL());

            File libDir = new File(parentFile, PATH_LIBRARY);
            addJars(appName, libDir, true);
            libraryPath = libDir.getAbsolutePath();

            // Add per-app mule modules (if any)
            File libs = new File(muleHome, PATH_LIBRARY);
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
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        return super.loadClass(name, resolve);
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

    @Override
    public void close()
    {
        for (ShutdownListener listener : shutdownListeners)
        {
            try
            {
                listener.execute();
            }
            catch (Exception e)
            {
                logger.error(e);
            }
        }
        super.close();
    }

    public void addShutdownListener(ShutdownListener listener)
    {
        this.shutdownListeners.add(listener);
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

    /**
     * Optional hook, invoked synchronously right before the classloader is disposed of and closed.
     */
    public interface ShutdownListener
    {
        void execute();
    }

    @Override
    protected String findLibrary(String name)
    {
        String parentResolvedPath = super.findLibrary(name);

        if (null == parentResolvedPath)
        {
            final File library = new File(libraryPath, System.mapLibraryName(name));

            if (library.exists())
            {
                parentResolvedPath = library.getAbsolutePath();
            }
        }

        return parentResolvedPath;
    }
}
