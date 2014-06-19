/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.mule.module.launcher.DisposableClassLoader;
import org.mule.module.launcher.MuleApplicationClassLoader;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.module.launcher.artifact.ShutdownListener;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
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
public class CompositeApplicationClassLoader extends ClassLoader implements ApplicationClassLoader
{

    protected static final Log logger = LogFactory.getLog(CompositeApplicationClassLoader.class);

    private final List<ClassLoader> classLoaders;
    private final String appName;

    public CompositeApplicationClassLoader(String appName, List<ClassLoader> classLoaders)
    {
        this.appName = appName;
        this.classLoaders = new LinkedList<ClassLoader>(classLoaders);
    }

    @Override
    public String getAppName()
    {
        return appName;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        for (ClassLoader classLoader : classLoaders)
        {
            try
            {
                Class<?> aClass = classLoader.loadClass(name);
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Class '%s' loaded from classLoader '%s", name, classLoader));
                }

                return aClass;
            }
            catch (ClassNotFoundException e)
            {
                // Ignoring
            }
        }

        throw new ClassNotFoundException(String.format("Cannot load class '%s'", name));
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        for (ClassLoader classLoader : classLoaders)
        {
            try
            {
                Class<?> aClass = loadClass(classLoader, name, resolve);
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Class '%s' loaded from classLoader '%s", name, classLoader));
                }

                return aClass;
            }
            catch (ClassNotFoundException e)
            {
                // Ignoring
            }
        }

        throw new ClassNotFoundException(String.format("Cannot load class '%s'", name));
    }

    protected Class<?> loadClass(ClassLoader classLoader, String name, boolean resolve) throws ClassNotFoundException
    {
        try
        {
            Method loadClassMethod = findDeclaredMethod(classLoader, "loadClass", String.class, boolean.class);

            return (Class<?>) loadClassMethod.invoke(classLoader, name, resolve);
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logReflectionLoadingError(name, classLoader, e, "Class");
            }
        }

        throw new ClassNotFoundException(String.format("Cannot load class '%s'", name));
    }

    @Override
    public URL getResource(String name)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            URL resource = classLoader.getResource(name);

            if (resource != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Resource '%s' loaded from classLoader '%s", name, classLoader));
                }

                return resource;
            }
        }

        return null;
    }

    @Override
    public InputStream getResourceAsStream(String name)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            InputStream resourceAsStream = classLoader.getResourceAsStream(name);

            if (resourceAsStream != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Stream resource '%s' loaded from classLoader '%s", name, classLoader));
                }

                return resourceAsStream;
            }
        }

        return null;
    }

    @Override
    protected String findLibrary(String libname)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            String library = findLibrary(libname, classLoader);
            if (library != null)
            {

                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Library '%s' found in classLoader '%s", libname, classLoader));
                }

                return library;
            }
        }

        return null;
    }

    protected String findLibrary(String libname, ClassLoader classLoader)
    {
        try
        {
            Method findLibraryMethod = findDeclaredMethod(classLoader, "findLibrary", String.class);

            return (String) findLibraryMethod.invoke(classLoader, libname);
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logReflectionLoadingError(libname, classLoader, e, "Library");
            }
        }

        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException
    {
        final Map<String, URL> resources = new HashMap<String, URL>();

        for (ClassLoader classLoader : classLoaders)
        {
            Enumeration<URL> partialResources = classLoader.getResources(name);

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

    @Override
    public String getArtifactName()
    {
        return this.appName;
    }

    @Override
    public URL findResource(String name)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            URL resource = findResource(classLoader, name);

            if (resource != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Resource '%s' loaded from classLoader '%s", name, classLoader));
                }

                return resource;
            }
        }

        return null;
    }

    @Override
    public URL findLocalResource(String resourceName)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            if( classLoader instanceof ArtifactClassLoader )
            {
                URL resource = ((ArtifactClassLoader)classLoader).findLocalResource(resourceName);
                if( resource!=null )
                {
                    return resource;
                }
            }
        }
        return null;
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return this;
    }

    @Override
    public void dispose()
    {
        for (ClassLoader classLoader : classLoaders)
        {
            if (classLoader instanceof DisposableClassLoader)
            {
                ((DisposableClassLoader) classLoader).dispose();
            }
        }
    }

    @Override
    public void addShutdownListener(ShutdownListener listener)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            if (classLoader instanceof MuleApplicationClassLoader)
            {
                ((ApplicationClassLoader)classLoader).addShutdownListener(listener);
                return;
            }
        }
    }

    private URL findResource(ClassLoader classLoader, String name)
    {
        try
        {
            Method findResourceMethod = findDeclaredMethod(classLoader, "findResource", String.class);

            return (URL) findResourceMethod.invoke(classLoader, name);
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logReflectionLoadingError(name, classLoader, e, "Resource");
            }
        }

        return null;
    }

    private Method findDeclaredMethod(ClassLoader classLoader, String methodName, Class<?>... params) throws NoSuchMethodException
    {
        Class clazz = classLoader.getClass();

        while (clazz != null)
        {
            try
            {
                Method findLibraryMethod = clazz.getDeclaredMethod(methodName, params);
                findLibraryMethod.setAccessible(true);

                return findLibraryMethod;
            }
            catch (NoSuchMethodException e)
            {
                clazz = clazz.getSuperclass();
            }
        }

        throw new NoSuchMethodException(String.format("Cannot find a method '%s' with the given parameter types '%s'", methodName, Arrays.toString(params)));
    }

    private void logReflectionLoadingError(String name, ClassLoader classLoader, Exception e, String type)
    {
        if (e instanceof InvocationTargetException && ((InvocationTargetException) e).getTargetException() instanceof ClassNotFoundException)
        {
            logger.debug(String.format("'%s' '%s' not found in class loader '%s'", type, name, classLoader));
        }
        else
        {
            final String errorMessage;
            if (e instanceof InvocationTargetException)
            {
                errorMessage = ((InvocationTargetException) e).getTargetException().getMessage();
            }
            else
            {
                errorMessage = e.getMessage();
            }

            logger.debug(String.format("Error loading '%s' '%s' from class loader '%s': '%s'", type.toLowerCase(), name, classLoader, errorMessage));
        }
    }
}
