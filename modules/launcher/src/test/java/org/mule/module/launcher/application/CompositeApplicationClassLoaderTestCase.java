/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.module.launcher.DisposableClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

@SmallTest
public class CompositeApplicationClassLoaderTestCase extends AbstractMuleTestCase
{

    public static final String CLASS_NAME = "java.lang.Object";
    public static final Class APP_LOADED_CLASS = Object.class;
    public static final Class PLUGIN_LOADED_CLASS = String.class;

    public static final String RESOURCE_NAME = "dummy.txt";
    public static final String PLUGIN_RESOURCE_NAME = "plugin.txt";

    public static final String LIBRARY_NAME = "dummy.so";
    public static final String APP_LOADED_LIBRARY = "app.dummy.so";
    public static final String PLUGIN_LOADED_LIBRARY = "plugin.dummy.so";
    public static final String APP_NAME = "testApp";

    public final URL APP_LOADED_RESOURCE;
    public final URL PLUGIN_LOADED_RESOURCE;

    public final InputStream APP_LOADED_STREAM_RESOURCE = mock(InputStream.class);
    public final InputStream PLUGIN_LOADED_STREAM_RESOURCE = mock(InputStream.class);
    private final TestApplicationClassLoader appClassLoader = new TestApplicationClassLoader();
    private final TestClassLoader pluginClassLoader = new SubTestClassLoader();


    public CompositeApplicationClassLoaderTestCase() throws MalformedURLException
    {
        APP_LOADED_RESOURCE = new URL("file:///app.txt");
        PLUGIN_LOADED_RESOURCE = new URL("file:///plugin.txt");
    }

    @Test
    public void loadsClassFromAppFirst() throws Exception
    {
        appClassLoader.addClass(CLASS_NAME, APP_LOADED_CLASS);
        pluginClassLoader.addClass(CLASS_NAME, PLUGIN_LOADED_CLASS);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        Class<?> aClass = compositeApplicationClassLoader.loadClass(CLASS_NAME);
        assertThat(aClass, equalTo(APP_LOADED_CLASS));
    }

    @Test
    public void loadsClassFromPluginWhenIsNotDefinedInApp() throws Exception
    {
        pluginClassLoader.addClass(CLASS_NAME, PLUGIN_LOADED_CLASS);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        Class<?> aClass = compositeApplicationClassLoader.loadClass(CLASS_NAME);
        assertThat(aClass, equalTo(PLUGIN_LOADED_CLASS));
    }

    @Test(expected = ClassNotFoundException.class)
    public void failsToLoadClassWhenIsNotDefinedInAnyClassLoader() throws Exception
    {
        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        compositeApplicationClassLoader.loadClass(CLASS_NAME);
    }

    @Test
    public void loadsResourceFromAppFirst() throws Exception
    {
        appClassLoader.addResource(RESOURCE_NAME, APP_LOADED_RESOURCE);
        pluginClassLoader.addResource(RESOURCE_NAME, PLUGIN_LOADED_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        URL resource = compositeApplicationClassLoader.getResource(RESOURCE_NAME);
        assertThat(resource, equalTo(APP_LOADED_RESOURCE));
    }

    @Test
    public void loadsResourceFromPluginWhenIsNotDefinedInApp() throws Exception
    {
        pluginClassLoader.addResource(RESOURCE_NAME, PLUGIN_LOADED_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        URL resource = compositeApplicationClassLoader.getResource(RESOURCE_NAME);
        assertThat(resource, equalTo(PLUGIN_LOADED_RESOURCE));
    }

    @Test
    public void returnsNullResourceWhenIsNotDefinedInAnyClassLoader() throws Exception
    {
        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        URL resource = compositeApplicationClassLoader.getResource(RESOURCE_NAME);
        assertThat(resource, equalTo(null));
    }

    @Test
    public void loadsStreamResourceFromAppFirst() throws Exception
    {
        appClassLoader.addStreamResource(RESOURCE_NAME, APP_LOADED_STREAM_RESOURCE);
        pluginClassLoader.addStreamResource(RESOURCE_NAME, PLUGIN_LOADED_STREAM_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        InputStream resourceAsStream = compositeApplicationClassLoader.getResourceAsStream(RESOURCE_NAME);
        assertThat(resourceAsStream, equalTo(APP_LOADED_STREAM_RESOURCE));
    }

    @Test
    public void loadsStreamResourceFromPluginWhenIsNotDefinedInApp() throws Exception
    {
        pluginClassLoader.addStreamResource(RESOURCE_NAME, PLUGIN_LOADED_STREAM_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        InputStream resourceAsStream = compositeApplicationClassLoader.getResourceAsStream(RESOURCE_NAME);
        assertThat(resourceAsStream, equalTo(PLUGIN_LOADED_STREAM_RESOURCE));
    }

    @Test
    public void returnsNullStreamResourceWhenIsNotDefinedInAnyClassLoader() throws Exception
    {
        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        InputStream resourceAsStream = compositeApplicationClassLoader.getResourceAsStream(RESOURCE_NAME);
        assertThat(resourceAsStream, equalTo(null));
    }

    @Test
    public void getsResourcesFromAppAndPluginClassLoader() throws Exception
    {
        appClassLoader.addResource(RESOURCE_NAME, APP_LOADED_RESOURCE);
        pluginClassLoader.addResource(PLUGIN_RESOURCE_NAME, PLUGIN_LOADED_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        Enumeration<URL> resources = compositeApplicationClassLoader.getResources(RESOURCE_NAME);

        List<URL> expectedResources = new LinkedList<URL>();
        expectedResources.add(APP_LOADED_RESOURCE);
        expectedResources.add(PLUGIN_LOADED_RESOURCE);

        assertThat(resources, EnumerationMatcher.equalTo(expectedResources));
    }

    @Test
    public void filtersResourcesDuplicatedInAppAndPluginClassLoader() throws Exception
    {
        appClassLoader.addResource(RESOURCE_NAME, APP_LOADED_RESOURCE);
        pluginClassLoader.addResource(RESOURCE_NAME, APP_LOADED_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        Enumeration<URL> resources = compositeApplicationClassLoader.getResources(RESOURCE_NAME);

        List<URL> expectedResources = new LinkedList<URL>();
        expectedResources.add(APP_LOADED_RESOURCE);

        assertThat(resources, EnumerationMatcher.equalTo(expectedResources));
    }

    @Test
    public void loadsLibraryFromAppFirst() throws Exception
    {
        appClassLoader.addLibrary(LIBRARY_NAME, APP_LOADED_LIBRARY);
        pluginClassLoader.addLibrary(LIBRARY_NAME, PLUGIN_LOADED_LIBRARY);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        String library = compositeApplicationClassLoader.findLibrary(LIBRARY_NAME);

        assertThat(library, equalTo(APP_LOADED_LIBRARY));
    }

    @Test
    public void loadsLibraryFromPluginWhenIsNotDefinedInApp() throws Exception
    {
        pluginClassLoader.addLibrary(LIBRARY_NAME, PLUGIN_LOADED_LIBRARY);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        String library = compositeApplicationClassLoader.findLibrary(LIBRARY_NAME);

        assertThat(library, equalTo(PLUGIN_LOADED_LIBRARY));
    }

    @Test
    public void returnsNullWhenLibraryIsNotDefinedInAnyClassLoader() throws Exception
    {
        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        String library = compositeApplicationClassLoader.findLibrary(LIBRARY_NAME);

        assertThat(library, equalTo(null));
    }

    @Test
    public void loadsResolvedClassFromAppFirst() throws Exception
    {
        appClassLoader.addClass(CLASS_NAME, APP_LOADED_CLASS);
        pluginClassLoader.addClass(CLASS_NAME, PLUGIN_LOADED_CLASS);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        Class<?> aClass = compositeApplicationClassLoader.loadClass(CLASS_NAME, true);
        assertThat(aClass, equalTo(APP_LOADED_CLASS));
    }

    @Test
    public void loadsResolvedClassFromPluginWhenIsNotDefinedInApp() throws Exception
    {
        pluginClassLoader.addClass(CLASS_NAME, PLUGIN_LOADED_CLASS);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        Class<?> aClass = compositeApplicationClassLoader.loadClass(CLASS_NAME, true);
        assertThat(aClass, equalTo(PLUGIN_LOADED_CLASS));
    }

    @Test(expected = ClassNotFoundException.class)
    public void failsToLoadResolvedClassWhenIsNotDefinedInAnyClassLoader() throws Exception
    {
        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        compositeApplicationClassLoader.loadClass(CLASS_NAME, true);
    }

    @Test
    public void findsResourceInAppFirst() throws Exception
    {
        appClassLoader.addResource(RESOURCE_NAME, APP_LOADED_RESOURCE);
        pluginClassLoader.addResource(RESOURCE_NAME, PLUGIN_LOADED_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        URL resource = compositeApplicationClassLoader.findResource(RESOURCE_NAME);
        assertThat(resource, equalTo(APP_LOADED_RESOURCE));
    }

    @Test
    public void findsResourceInPluginWhenIsNotDefinedInApp() throws Exception
    {
        pluginClassLoader.addResource(RESOURCE_NAME, PLUGIN_LOADED_RESOURCE);

        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        URL resource = compositeApplicationClassLoader.findResource(RESOURCE_NAME);
        assertThat(resource, equalTo(PLUGIN_LOADED_RESOURCE));
    }

    @Test
    public void returnsNullFindingResourceWhenIsNotDefinedInAnyClassLoader() throws Exception
    {
        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        URL resource = compositeApplicationClassLoader.findResource(RESOURCE_NAME);
        assertThat(resource, equalTo(null));
    }

    @Test
    public void disposesApplicationClassLoaders() throws Exception
    {
        List<ClassLoader> classLoaders = getClassLoaders(appClassLoader, pluginClassLoader);

        CompositeApplicationClassLoader compositeApplicationClassLoader = new CompositeApplicationClassLoader(APP_NAME, classLoaders);

        compositeApplicationClassLoader.dispose();
        assertThat(appClassLoader.disposed, equalTo(true));
    }

    private List<ClassLoader> getClassLoaders(ClassLoader... expectedClassLoaders)
    {
        List<ClassLoader> classLoaders = new LinkedList<ClassLoader>();

        Collections.addAll(classLoaders, expectedClassLoaders);

        return classLoaders;
    }

    public static class TestApplicationClassLoader extends TestClassLoader implements DisposableClassLoader
    {

        private boolean disposed;

        @Override
        public void dispose()
        {
            this.disposed = true;
        }
    }

    // Used to ensure that the composite classloader is able to access
    // protected methods in subclasses by reflection
    public static class SubTestClassLoader extends TestClassLoader
    {

    }
}
