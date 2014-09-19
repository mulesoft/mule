/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.util.ClassUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Collection;
import java.util.jar.JarFile;

import sun.net.www.protocol.jar.Handler;

/**
 * Fixes major classloader woes by:
 * <ol>
 *  <li>Providing a {@link #dispose()} method to release any connections to resources.</li>
 *  <li>Disabling caching of jar resources fix e.g. java.util.ResourceBundle 'tagging' the app
 *      and preventing it from being undeployed correctly (no leaving locked jars behind).</li>
 * </ol>
 */
public class GoodCitizenClassLoader extends URLClassLoader implements DisposableClassLoader
{

    public GoodCitizenClassLoader(URL[] urls, ClassLoader parent)
    {
        super(urls, parent, new NonCachingURLStreamHandlerFactory());
    }

    /**
     * A workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5041014
     */
    @Override
    public void dispose()
    {
        // jars
        try
        {
            Class<URLClassLoader> clazz = URLClassLoader.class;
            Field ucp = clazz.getDeclaredField("ucp");
            ucp.setAccessible(true);
            Object urlClassPath = ucp.get(this);
            Field loaders = urlClassPath.getClass().getDeclaredField("loaders");
            loaders.setAccessible(true);
            Collection<?> jarLoaders = (Collection<?>) loaders.get(urlClassPath);
            for (Object jarLoader : jarLoaders)
            {
                try
                {
                    Field loader = jarLoader.getClass().getDeclaredField("jar");
                    loader.setAccessible(true);
                    Object jarFile = loader.get(jarLoader);
                    ((JarFile) jarFile).close();
                }
                catch (Throwable t)
                {
                    // if we got this far, this is probably not a JAR loader so skip it
                }
            }
        }
        catch (Throwable t)
        {
            // probably not a SUN VM
        }

        try
        {
            // fix groovy compiler leaks http://www.mulesoft.org/jira/browse/MULE-5125
            final Class clazz = ClassUtils.loadClass("org.codehaus.groovy.transform.ASTTransformationVisitor", getClass());
            final Field compUnit = clazz.getDeclaredField("compUnit");
            compUnit.setAccessible(true);
            // static field
            compUnit.set(null, null);
        }
        catch (Throwable t)
        {
            // ignore
        }
    }

    protected static class NonCachingURLStreamHandlerFactory implements URLStreamHandlerFactory
    {
        public URLStreamHandler createURLStreamHandler(String protocol)
        {
            return new NonCachingJarResourceURLStreamHandler();
        }
    }

    /**
     * Prevents jar caching for this classloader, mainly to fix the static ResourceBundle mess/cache
     * that keeps connections open no matter what.
     */
    private static class NonCachingJarResourceURLStreamHandler extends Handler
    {
        public NonCachingJarResourceURLStreamHandler()
        {
            super();
        }

        @Override
        protected java.net.URLConnection openConnection(URL u) throws IOException
        {
            JarURLConnection c = new sun.net.www.protocol.jar.JarURLConnection(u, this);
            c.setUseCaches(false);
            return c;
        }
    }
}
