/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.api.config.MuleProperties;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MuleApplicationClassLoader extends GoodCitizenClassLoader
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

    protected List<ShutdownListener> shutdownListeners = new ArrayList<ShutdownListener>();

    private String appName;

    public MuleApplicationClassLoader(String appName, ClassLoader parentCl)
    {
        super(CLASSPATH_EMPTY, parentCl);
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

            if (libDir.exists() && libDir.canRead())
            {
                @SuppressWarnings("unchecked")
                Collection<File> jars = FileUtils.listFiles(libDir, new String[] {"jar"}, false);

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
}
