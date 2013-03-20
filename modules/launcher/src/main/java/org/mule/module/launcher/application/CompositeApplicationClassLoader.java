/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.application;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines a classloader that delegates classes and resources resolution to
 * a list of classloaders.
 */
public class CompositeApplicationClassLoader extends ClassLoader
{

    protected static final Log logger = LogFactory.getLog(CompositeApplicationClassLoader.class);

    private final List<ClassLoader> classLoaders;

    public CompositeApplicationClassLoader(List<ClassLoader> classLoaders)
    {
        this.classLoaders = new LinkedList<ClassLoader>(classLoaders);
    }

    @Override
    public Class<?> loadClass(String s) throws ClassNotFoundException
    {
        for (ClassLoader classLoader : classLoaders)
        {
            try
            {
                Class<?> aClass = classLoader.loadClass(s);
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Class '%s' loaded from classLoader '%s", s, classLoader));
                }

                return aClass;
            }
            catch (ClassNotFoundException e)
            {
                // Ignoring
            }
        }

        throw new ClassNotFoundException(String.format("Cannot load class '%s'", s));
    }

    @Override
    protected synchronized Class<?> loadClass(String s, boolean b) throws ClassNotFoundException
    {
        for (ClassLoader classLoader : classLoaders)
        {
            try
            {
                Class<?> aClass = loadClass(classLoader, s, b);
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Class '%s' loaded from classLoader '%s", s, classLoader));
                }

                return aClass;
            }
            catch (ClassNotFoundException e)
            {
                // Ignoring
            }
        }

        throw new ClassNotFoundException(String.format("Cannot load class '%s'", s));
    }

    protected Class<?> loadClass(ClassLoader classLoader, String s, boolean b) throws ClassNotFoundException
    {
        try
        {
            Method findLibraryMethod = classLoader.getClass().getDeclaredMethod("loadClass", String.class, boolean.class);
            findLibraryMethod.setAccessible(true);

            return (Class<?>) findLibraryMethod.invoke(classLoader, s, b);
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Error loading class '%s' from classloader", s, classLoader), e);
            }
        }

        throw new ClassNotFoundException(String.format("Cannot load class '%s'", s));
    }

    @Override
    public URL getResource(String s)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            URL resource = classLoader.getResource(s);

            if (resource != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Resource '%s' loaded from classLoader '%s", s, classLoader));
                }

                return resource;
            }
        }

        return null;
    }

    @Override
    public InputStream getResourceAsStream(String s)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            InputStream resourceAsStream = classLoader.getResourceAsStream(s);

            if (resourceAsStream != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Stream resource '%s' loaded from classLoader '%s", s, classLoader));
                }

                return resourceAsStream;
            }
        }

        return null;
    }

    @Override
    protected String findLibrary(String s)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            String library = findLibrary(s, classLoader);
            if (library != null)
            {

                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Library '%s' found in classLoader '%s", s, classLoader));
                }

                return library;
            }
        }

        return null;
    }

    protected String findLibrary(String s, ClassLoader classLoader)
    {
        try
        {
            Method findLibraryMethod = classLoader.getClass().getDeclaredMethod("findLibrary", String.class);
            findLibraryMethod.setAccessible(true);

            return (String) findLibraryMethod.invoke(classLoader, s);
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Error finding library '%s' in classloader", s, classLoader), e);
            }
        }

        return null;
    }

    @Override
    public Enumeration<URL> getResources(String s) throws IOException
    {
        final Map<String, URL> resources = new HashMap<String, URL>();

        for (ClassLoader classLoader : classLoaders)
        {
            Enumeration<URL> partialResources = classLoader.getResources(s);

            while (partialResources.hasMoreElements())
            {
                URL url = partialResources.nextElement();
                if (resources.get(url.toString()) == null)
                {
                    resources.put(url.toString(), url);
                }
            }
        }

        return new EnumerationAdapter<URL>(resources.values());
    }
}
