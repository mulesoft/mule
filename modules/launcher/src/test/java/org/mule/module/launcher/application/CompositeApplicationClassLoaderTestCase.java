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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

@SmallTest
public class CompositeApplicationClassLoaderTestCase extends AbstractMuleTestCase
{

    public static final String CLASS_NAME = "java.lang.Object";
    public static final Class APP_LOADED_CLASS = Object.class;
    public static final Class PLUGIN_LOADED_CLASS = String.class;

    public static final String RESOURCE_NAME = "dummy.txt";
    public static final String PLUGIN_RESOURCE_NAME = "plugin.txt";
    public final URL APP_LOADED_RESOURCE;
    public final URL PLUGIN_LOADED_RESOURCE;

    public final InputStream APP_LOADED_STREAM_RESOURCE = mock(InputStream.class);
    public final InputStream PLUGIN_LOADED_STREAM_RESOURCE = mock(InputStream.class);
    private final TestClassLoader appClassLoader = new TestClassLoader();
    private final TestClassLoader pluginClassLoader = new TestClassLoader();


    public CompositeApplicationClassLoaderTestCase() throws MalformedURLException
    {
        APP_LOADED_RESOURCE = new URL("file:///app.txt");
        PLUGIN_LOADED_RESOURCE = new URL("file:///plugin.txt");
    }

    @Test
    public void loadsClassFromAppFirst() throws Exception
    {
        appClassLoader.classes.put(CLASS_NAME, APP_LOADED_CLASS);
        pluginClassLoader.classes.put(CLASS_NAME, PLUGIN_LOADED_CLASS);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(classLoaders);

        Class<?> aClass = compositeApplicationClassLoader.loadClass(CLASS_NAME);
        assertThat(aClass, equalTo(APP_LOADED_CLASS));
    }

    @Test
    public void loadsClassFromPluginWhenIsNotDefinedInApp() throws Exception
    {
        pluginClassLoader.classes.put(CLASS_NAME, PLUGIN_LOADED_CLASS);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(classLoaders);

        Class<?> aClass = compositeApplicationClassLoader.loadClass(CLASS_NAME);
        assertThat(aClass, equalTo(PLUGIN_LOADED_CLASS));
    }

    @Test(expected = ClassNotFoundException.class)
    public void failsToLoadClassWhenIsNotDefinedInAnyClassLoader() throws Exception
    {
        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(classLoaders);

        compositeApplicationClassLoader.loadClass(CLASS_NAME);
    }

    @Test
    public void loadsResourceFromAppFirst() throws Exception
    {
        appClassLoader.resources.put(RESOURCE_NAME, APP_LOADED_RESOURCE);
        pluginClassLoader.resources.put(RESOURCE_NAME, PLUGIN_LOADED_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(classLoaders);

        URL resource = compositeApplicationClassLoader.getResource(RESOURCE_NAME);
        assertThat(resource, equalTo(APP_LOADED_RESOURCE));
    }

    @Test
    public void loadsResourceFromPluginWhenIsNotDefinedInApp() throws Exception
    {
        pluginClassLoader.resources.put(RESOURCE_NAME, PLUGIN_LOADED_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(classLoaders);

        URL resource = compositeApplicationClassLoader.getResource(RESOURCE_NAME);
        assertThat(resource, equalTo(PLUGIN_LOADED_RESOURCE));
    }

    @Test
    public void returnsNullResourceWhenIsNotDefinedInAnyClassLoader() throws Exception
    {
        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(classLoaders);

        URL resource = compositeApplicationClassLoader.getResource(RESOURCE_NAME);
        assertThat(resource, equalTo(null));
    }

    @Test
    public void loadsStreamResourceFromAppFirst() throws Exception
    {
        appClassLoader.streamResources.put(RESOURCE_NAME, APP_LOADED_STREAM_RESOURCE);
        pluginClassLoader.streamResources.put(RESOURCE_NAME, PLUGIN_LOADED_STREAM_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(classLoaders);

        InputStream resourceAsStream = compositeApplicationClassLoader.getResourceAsStream(RESOURCE_NAME);
        assertThat(resourceAsStream, equalTo(APP_LOADED_STREAM_RESOURCE));
    }

    @Test
    public void loadsStreamResourceFromPluginWhenIsNotDefinedInApp() throws Exception
    {
        pluginClassLoader.streamResources.put(RESOURCE_NAME, PLUGIN_LOADED_STREAM_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(classLoaders);

        InputStream resourceAsStream = compositeApplicationClassLoader.getResourceAsStream(RESOURCE_NAME);
        assertThat(resourceAsStream, equalTo(PLUGIN_LOADED_STREAM_RESOURCE));
    }

    @Test
    public void returnsNullStreamResourceWhenIsNotDefinedInAnyClassLoader() throws Exception
    {
        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(classLoaders);

        InputStream resourceAsStream = compositeApplicationClassLoader.getResourceAsStream(RESOURCE_NAME);
        assertThat(resourceAsStream, equalTo(null));
    }

    @Test
    public void getsResourcesFromAppAndPluginClassLoader() throws Exception
    {
        appClassLoader.resources.put(RESOURCE_NAME, APP_LOADED_RESOURCE);
        pluginClassLoader.resources.put(PLUGIN_RESOURCE_NAME, PLUGIN_LOADED_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(classLoaders);

        Enumeration<URL> resources = compositeApplicationClassLoader.getResources(RESOURCE_NAME);

        List<URL> expectedResources = new LinkedList<URL>();
        expectedResources.add(APP_LOADED_RESOURCE);
        expectedResources.add(PLUGIN_LOADED_RESOURCE);

        assertThat(resources, EnumerationMatcher.equalTo(expectedResources));
    }

    @Test
    public void filtersResourcesDuplicatedInAppAndPluginClassLoader() throws Exception
    {
        appClassLoader.resources.put(RESOURCE_NAME, APP_LOADED_RESOURCE);
        pluginClassLoader.resources.put(RESOURCE_NAME, APP_LOADED_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(classLoaders);

        Enumeration<URL> resources = compositeApplicationClassLoader.getResources(RESOURCE_NAME);

        List<URL> expectedResources = new LinkedList<URL>();
        expectedResources.add(APP_LOADED_RESOURCE);

        assertThat(resources, EnumerationMatcher.equalTo(expectedResources));
    }

    private List<ClassLoader> getClassLoaders(ClassLoader... expectedClassLoaders)
    {
        List<ClassLoader> classLoaders = new LinkedList<ClassLoader>();

        Collections.addAll(classLoaders, expectedClassLoaders);

        return classLoaders;
    }

    public static class TestClassLoader extends ClassLoader
    {

        private Map<String, Class> classes = new HashMap<String, Class>();
        private Map<String, URL> resources = new HashMap<String, URL>();
        private Map<String, InputStream> streamResources = new HashMap<String, InputStream>();

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
    }
}
