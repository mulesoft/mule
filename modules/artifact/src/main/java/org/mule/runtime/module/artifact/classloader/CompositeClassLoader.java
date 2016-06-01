/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_ONLY;

import org.mule.runtime.module.artifact.classloader.exception.CompositeClassNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
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
 * <p/>
 * By using a {@link ClassLoaderLookupPolicy} this classLoader can use
 * parent-first, parent-only or child-first classloading lookup mode per package.
 */
public class CompositeClassLoader extends ClassLoader implements ClassLoaderLookupPolicyProvider
{

    protected static final Log logger = LogFactory.getLog(CompositeClassLoader.class);

    protected final List<ClassLoader> classLoaders;
    private final ClassLoaderLookupPolicy lookupPolicy;

    /**
     * Creates a new instance
     *
     * @param parent parent class loader used to delegate the lookup process. Can be null.
     * @param classLoaders class loaders to compose. Non empty.
     * @param lookupPolicy policy used to guide the lookup process. Non null
     */
    public CompositeClassLoader(ClassLoader parent, List<ClassLoader> classLoaders, ClassLoaderLookupPolicy lookupPolicy)
    {
        super(parent);
        checkArgument(classLoaders != null && !classLoaders.isEmpty(), "Classloaders must have at least a classLoader");
        checkArgument(lookupPolicy != null, "Lookup policy cannot be null");
        this.lookupPolicy = lookupPolicy;
        this.classLoaders = new LinkedList<>(classLoaders);
    }

    @Override
    public ClassLoaderLookupPolicy getClassLoaderLookupPolicy()
    {
        return lookupPolicy;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        return loadClass(name, false);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        final ClassLoaderLookupStrategy lookupStrategy = lookupPolicy.getLookupStrategy(name);
        Class<?> result;

        /*
         * Gather information about the exceptions in each of the contained classloaders to provide
         * troubleshooting information in case of throwing a ClassNotFoundException.
         */
        final List<ClassNotFoundException> exceptions = new ArrayList<>(classLoaders.size() + 1);

        if (lookupStrategy == PARENT_ONLY)
        {
            result = getParent().loadClass(name);
        }
        else if (lookupStrategy == PARENT_FIRST)
        {
            try
            {
                result = getParent().loadClass(name);
            }
            catch (ClassNotFoundException e)
            {
                exceptions.add(e);
                result = doLoadClass(name, exceptions);
            }
        }
        else
        {
            result = doLoadClass(name, exceptions);
            if (result == null)
            {
                result = getParent().loadClass(name);
            }
        }

        if (result != null)
        {
            if (resolve)
            {
                resolveClass(result);
            }

            return result;
        }

        throw new CompositeClassNotFoundException(name, lookupStrategy, exceptions);
    }

    private Class<?> doLoadClass(String name, List<ClassNotFoundException> exceptions)
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
                exceptions.add(e);
            }
        }
        return null;
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
        final Map<String, URL> resources = new HashMap<>();

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

        return new EnumerationAdapter<>(resources.values());
    }


    protected Method findDeclaredMethod(ClassLoader classLoader, String methodName, Class<?>... params) throws NoSuchMethodException
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

    protected void logReflectionLoadingError(String name, ClassLoader classLoader, Exception e, String type)
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
