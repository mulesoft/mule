/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MuleApplicationClassLoader extends URLClassLoader
{

    /**
     * Library directory in Mule application.
     */
    public static final String PATH_LIBRARY = "lib";

    /**
     * Classes and resources directory in Mule application.
     */
    public static final String PATH_CLASSES = "classes";

    protected static final URL[] CLASSPATH_EMPTY = new URL[0];
    protected final transient Log logger = LogFactory.getLog(getClass());
    private File monitoredResource;
    private String appName;

    // TODO refactor to use an app name instead and use convention for monitoredResource
    public MuleApplicationClassLoader(String appName, File monitoredResource, ClassLoader parentCl)
    {
        super(CLASSPATH_EMPTY, parentCl);
        this.appName = appName;
        this.monitoredResource = monitoredResource;
        try
        {
            // get lib dir on the same level as monitored resource and...
            File parentFile = monitoredResource.getParentFile();
            File classesDir = new File(parentFile, PATH_CLASSES);
            addURL(classesDir.toURI().toURL());

            File libDir = new File(parentFile, PATH_LIBRARY);

            if (logger.isInfoEnabled())
            {
                logger.info(String.format("[%s] Library directory: %s", appName, libDir));
            }

            if (libDir.exists() && libDir.canRead())
            {
                @SuppressWarnings("unchecked")
                Collection<File> jars = FileUtils.listFiles(libDir, new String[] {"jar"}, false);

                if (!jars.isEmpty() && logger.isInfoEnabled())
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("[%s] Loading the following jars:", appName)).append(SystemUtils.LINE_SEPARATOR);
                    sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                    for (File jar : jars)
                    {
                        sb.append(jar.toURI().toURL()).append(SystemUtils.LINE_SEPARATOR);
                    }

                    sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                    logger.info(sb.toString());
                }

                for (File jar : jars)
                {
                    addURL(jar.toURI().toURL());
                }
            }

        }
        catch (IOException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("[%s]", appName), e);
            }
        }
    }

    public File getMonitoredResource()
    {
        return this.monitoredResource;
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
}
