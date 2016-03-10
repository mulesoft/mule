/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class CompositeClassLoaderTestCase extends AbstractMuleTestCase
{

    public static final String CLASS_NAME = "java.lang.Object";
    public static final Class APP_LOADED_CLASS = Object.class;
    public static final Class PLUGIN_LOADED_CLASS = String.class;

    public static final String LIBRARY_NAME = "dummy.so";
    public static final String APP_LOADED_LIBRARY = "app.dummy.so";
    public static final String PLUGIN_LOADED_LIBRARY = "plugin.dummy.so";

    private final TestClassLoader classLoader1 = new TestClassLoader();
    private final TestClassLoader classLoader2 = new SubTestClassLoader();

    @Test
    public void loadsLibraryFromFirstClassLoader() throws Exception
    {
        classLoader1.addLibrary(LIBRARY_NAME, APP_LOADED_LIBRARY);
        classLoader2.addLibrary(LIBRARY_NAME, PLUGIN_LOADED_LIBRARY);

        CompositeClassLoader compositeClassLoader = createCompositeClassLoader();

        String library = compositeClassLoader.findLibrary(LIBRARY_NAME);

        assertThat(library, equalTo(APP_LOADED_LIBRARY));
    }

    @Test
    public void loadsLibraryFromSecondClassLoaderWhenIsNotDefinedOnTheFirstOne() throws Exception
    {
        classLoader2.addLibrary(LIBRARY_NAME, PLUGIN_LOADED_LIBRARY);

        CompositeClassLoader CompositeClassLoader = createCompositeClassLoader();

        String library = CompositeClassLoader.findLibrary(LIBRARY_NAME);

        assertThat(library, equalTo(PLUGIN_LOADED_LIBRARY));
    }

    @Test
    public void returnsNullWhenLibraryIsNotDefinedInAnyClassLoader() throws Exception
    {
        CompositeClassLoader CompositeClassLoader = createCompositeClassLoader();

        String library = CompositeClassLoader.findLibrary(LIBRARY_NAME);

        assertThat(library, equalTo(null));
    }

    @Test
    public void loadsResolvedClassFromFirstClassLoader() throws Exception
    {
        classLoader1.addClass(CLASS_NAME, APP_LOADED_CLASS);
        classLoader2.addClass(CLASS_NAME, PLUGIN_LOADED_CLASS);

        CompositeClassLoader CompositeClassLoader = createCompositeClassLoader();

        Class<?> aClass = CompositeClassLoader.loadClass(CLASS_NAME, true);
        assertThat(aClass, equalTo(APP_LOADED_CLASS));
    }

    @Test
    public void loadsResolvedClassFromSecondClassLoaderWhenIsNotDefinedOnTheFirstOne() throws Exception
    {
        classLoader2.addClass(CLASS_NAME, PLUGIN_LOADED_CLASS);

        CompositeClassLoader CompositeClassLoader = createCompositeClassLoader();

        Class<?> aClass = CompositeClassLoader.loadClass(CLASS_NAME, true);
        assertThat(aClass, equalTo(PLUGIN_LOADED_CLASS));
    }

    @Test(expected = ClassNotFoundException.class)
    public void failsToLoadResolvedClassWhenIsNotDefinedInAnyClassLoader() throws Exception
    {
        CompositeClassLoader CompositeClassLoader = createCompositeClassLoader();

        CompositeClassLoader.loadClass(CLASS_NAME, true);
    }

    private CompositeClassLoader createCompositeClassLoader()
    {
        List<ClassLoader> classLoaders = new LinkedList<>();
        Collections.addAll(classLoaders, classLoader1, classLoader2);

        return new CompositeClassLoader(Thread.currentThread().getContextClassLoader(), classLoaders);
    }

    // Used to ensure that the composite classloader is able to access
    // protected methods in subclasses by reflection
    public static class SubTestClassLoader extends TestClassLoader
    {

    }
}