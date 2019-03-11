/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.boot;

import static org.mule.util.FileUtils.deleteFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.List;

public final class MuleBootstrapUtils
{
    private static final String MULE_LIB_FILENAME = "lib" + File.separator + "mule";
    private static final String MULE_HOME = System.getProperty("mule.home");
    
    public static final String MULE_LOCAL_JAR_FILENAME = "mule-local-install.jar";

    private MuleBootstrapUtils()
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
        return MULE_HOME != null;
    }

    /**
     * @return null if running embedded
     */
    public static File getMuleHomeFile()
    {
        return isStandalone() ? new File(MULE_HOME) : null;
    }

    /**
     * @return null if running embedded
     */
    public static File getMuleLibDir()
    {   
        return isStandalone() ? new File(MULE_HOME + File.separator + MULE_LIB_FILENAME) : null;
    }
    
    public static File getMuleLocalJarFile()
    {
        return isStandalone() ? new File(getMuleLibDir(), MULE_LOCAL_JAR_FILENAME) : null;
    }
    
    public static void addLocalJarFilesToClasspath(File muleHome, File muleBase) throws Exception
    {
        DefaultMuleClassPathConfig classPath = new DefaultMuleClassPathConfig(muleHome, muleBase);
        addLibrariesToClasspath(classPath.getURLs());
    }
    
    public static void addLibrariesToClasspath(List urls) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        ClassLoader sys = ClassLoader.getSystemClassLoader();
        if (!(sys instanceof URLClassLoader))
        {
            throw new IllegalArgumentException(
                "PANIC: Mule has been started with an unsupported classloader: " + sys.getClass().getName()
                                + ". " + "Please report this error to user<at>mule<dot>codehaus<dot>org");
        }
    
        // system classloader is in this case the one that launched the application,
        // which is usually something like a JDK-vendor proprietary AppClassLoader
        URLClassLoader sysCl = (URLClassLoader) sys;
    
        /*
         * IMPORTANT NOTE: The more 'natural' way would be to create a custom
         * URLClassLoader and configure it, but then there's a chicken-and-egg
         * problem, as all classes MuleBootstrap depends on would have been loaded by
         * a parent classloader, and not ours. There's no straightforward way to
         * change this, and is documented in a Sun's classloader guide. The solution
         * would've involved overriding the ClassLoader.findClass() method and
         * modifying the semantics to be child-first, but that way we are calling for
         * trouble. Hacking the primordial classloader is a bit brutal, but works
         * perfectly in case of running from the command-line as a standalone app.
         * All Mule embedding options then delegate the classpath config to the
         * embedder (a developer embedding Mule in the app), thus classloaders are
         * not modified in those scenarios.
         */
    
        // get a Method ref from the normal class, but invoke on a proprietary parent
        // object,
        // as this method is usually protected in those classloaders
        Class refClass = URLClassLoader.class;
        Method methodAddUrl = refClass.getDeclaredMethod("addURL", new Class[]{URL.class});
        methodAddUrl.setAccessible(true);
        for (Iterator it = urls.iterator(); it.hasNext();)
        {
            URL url = (URL) it.next();
            methodAddUrl.invoke(sysCl, new Object[]{url});
        }
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
    public static URL getResource(final String resourceName, final Class callingClass)
    {
        URL url = (URL) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                return cl != null ? cl.getResource(resourceName) : null;
            }
        });

        if (url == null)
        {
            url = (URL) AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    return MuleBootstrap.class.getClassLoader().getResource(resourceName);
                }
            });
        }

        if (url == null)
        {
            url = (URL) AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
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
    public static int copy(InputStream input, OutputStream output) throws IOException {
        long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    /**
     * @see org.mule.util.IOUtils#copyLarge
     */
    public static long copyLarge(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
