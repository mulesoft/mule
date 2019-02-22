/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.reboot;

import static org.mule.util.FileUtils.deleteFile;
import org.mule.MuleServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

public final class MuleContainerBootstrapUtils
{
    public static final String MULE_DOMAIN_FOLDER = "domains";
    public static final String MULE_LOCAL_JAR_FILENAME = "mule-local-install.jar";
    private static final String MULE_APPS_FILENAME = "apps";
    private static final String MULE_LIB_FILENAME = "lib/mule";
    private static final String MULE_CONF_FILENAME = "conf";

    private MuleContainerBootstrapUtils()
    {
        // utility class only
    }

    /**
     * Whether Mule is running embedded or standalone.
     * @return true if running standalone
     */
    public static boolean isStandalone()
    {
        // when embedded, mule.home var is not set
        return getMuleHome() != null;
    }

    /**
     * @return null if running embedded
     */
    public static File getMuleHome()
    {
        final String muleHome = System.getProperty("mule.home");
        return muleHome != null ? new File(muleHome) : null;
    }

    /**
     * @return null if running embedded, otherwise the apps dir as a File ref
     */
    public static File getMuleAppsDir()
    {
        return isStandalone() ? new File(getMuleHome(), MULE_APPS_FILENAME) : null;
    }

    /**
     * @param appName name of the application
     * @return null if running embedded, otherwise the app dir as a File ref
     */
    public static File getMuleAppDir(String appName)
    {
        return isStandalone() ? new File(getMuleAppsDir(), appName) : null;
    }

    /**
     * @param appName name of the application
     * @return null if running embedded, otherwise the app default configuration file as a File ref
     */
    public static File getMuleAppDefaultConfigFile(String appName)
    {
        return isStandalone() ? new File(getMuleAppDir(appName), MuleServer.DEFAULT_CONFIGURATION) : null;
    }

    /**
     * @return null if running embedded
     */
    public static File getMuleLibDir()
    {
        return isStandalone() ? new File(getMuleHome(), MULE_LIB_FILENAME) : null;
    }

    public static File getMuleLocalJarFile()
    {
        return isStandalone() ? new File(getMuleLibDir(), MULE_LOCAL_JAR_FILENAME) : null;
    }

    public static File getMuleDomainsDir()
    {
        return isStandalone() ? new File(getMuleHome(), MULE_DOMAIN_FOLDER) : null;
    }

    /**
     * @return null if running embedded, otherwise the conf dir as a File ref
     */
    public static File getMuleConfDir()
    {
        return isStandalone() ? new File(getMuleHome(), MULE_CONF_FILENAME) : null;
    }

    public static class ProxyInfo
    {
        String host;
        String port;
        String username;
        String password;

        public ProxyInfo(String host, String port)
        {
            this(host, port, null, null);
        }

        public ProxyInfo(String host, String port, String username, String password)
        {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // The following methods are intentionally duplicated from org.mule.util so that
    // mule-module-boot has no external dependencies at system startup.
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @see org.mule.util.ClassUtils#getResource
     */
    public static URL getResource(final String resourceName, final Class<?> callingClass)
    {
        URL url = AccessController.doPrivileged(new PrivilegedAction<URL>()
        {
            public URL run()
            {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                return cl != null ? cl.getResource(resourceName) : null;
            }
        });

        if (url == null)
        {
            url = AccessController.doPrivileged(new PrivilegedAction<URL>()
            {
                public URL run()
                {
                    return MuleContainerBootstrap.class.getClassLoader().getResource(resourceName);
                }
            });
        }

        if (url == null)
        {
            url = AccessController.doPrivileged(new PrivilegedAction<URL>()
            {
                public URL run()
                {
                    return callingClass.getClassLoader().getResource(resourceName);
                }
            });
        }

        return url;
    }

    /**
     * @see org.mule.util.FileUtils#renameFile
     */
    public static boolean renameFile(File srcFile, File destFile) throws IOException
    {
        boolean isRenamed = false;
        if (srcFile != null && destFile != null)
        {
            if (!destFile.exists())
            {
                if (srcFile.isFile())
                {
                    isRenamed = srcFile.renameTo(destFile);
                    if (!isRenamed && srcFile.exists())
                    {
                        isRenamed = renameFileHard(srcFile, destFile);
                    }
                }
            }
        }
        return isRenamed;
    }

    /**
     * @see org.mule.util.FileUtils#renameFileHard
     */
    public static boolean renameFileHard(File srcFile, File destFile) throws IOException
    {
        boolean isRenamed = false;
        if (srcFile != null && destFile != null)
        {
            if (!destFile.exists())
            {
                if (srcFile.isFile())
                {
                    FileInputStream in = null;
                    FileOutputStream out = null;
                    try
                    {
                        in = new FileInputStream(srcFile);
                        out = new FileOutputStream(destFile);
                        out.getChannel().transferFrom(in.getChannel(), 0, srcFile.length());
                        isRenamed = true;
                    }
                    finally
                    {
                        if (in != null)
                        {
                            in.close();
                        }
                        if (out != null)
                        {
                            out.close();
                        }
                    }
                    if (isRenamed)
                    {
                        deleteFile(srcFile);
                    }
                    else
                    {
                        deleteFile(destFile);
                    }
                }
            }
        }
        return isRenamed;
    }

    /**
     * @see org.mule.util.IOUtils#copy
     */
    public static int copy(InputStream input, OutputStream output) throws IOException
    {
        long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE)
        {
            return -1;
        }
        return (int) count;
    }

    /**
     * @see org.mule.util.IOUtils#copyLarge
     */
    public static long copyLarge(InputStream input, OutputStream output) throws IOException
    {
        byte[] buffer = new byte[1024 * 4];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer)))
        {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
