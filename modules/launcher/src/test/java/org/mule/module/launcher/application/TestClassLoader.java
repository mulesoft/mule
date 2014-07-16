/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 *  Fake {@link ClassLoader} for testing purposes
 */
public class TestClassLoader extends ClassLoader
{

    private Map<String, Class> classes = new HashMap<String, Class>();
    private Map<String, URL> resources = new HashMap<String, URL>();
    private Map<String, InputStream> streamResources = new HashMap<String, InputStream>();
    private Map<String, String> libraries = new HashMap<String, String>();

    @Override
    public Class<?> loadClass(String s) throws ClassNotFoundException
    {

        Class aClass = classes.get(s);
        if (aClass == null)
        {
            throw new ClassNotFoundException(s);
        }

        return aClass;
    }

    @Override
    public URL getResource(String s)
    {
        return resources.get(s);
    }

    @Override
    public InputStream getResourceAsStream(String s)
    {
        return streamResources.get(s);
    }

    @Override
    public Enumeration<URL> getResources(String s) throws IOException
    {
        return new EnumerationAdapter<URL>(resources.values());
    }

    @Override
    protected URL findResource(String s)
    {
        return resources.get(s);
    }

    @Override
    protected String findLibrary(String s)
    {
        return libraries.get(s);
    }

    public void addClass(String className, Class aClass)
    {
        classes.put(className, aClass);
    }

    public void addResource(String resourceName, URL resourceUrl)
    {
        resources.put(resourceName, resourceUrl);
    }

    public void addStreamResource(String resourceName, InputStream resourceStream)
    {
        streamResources.put(resourceName, resourceStream);
    }

    public void addLibrary(String libraryName, String libraryPath)
    {
        libraries.put(libraryName, libraryPath);
    }

    @Override
    protected synchronized Class<?> loadClass(String s, boolean b) throws ClassNotFoundException
    {
        return loadClass(s);
    }
}
