/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_ONLY;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class CompositeClassLoaderTestCase extends AbstractMuleTestCase
{

    public static final String CLASS_PACKAGE = "java.lang";
    public static final String CLASS_NAME = "java.lang.Object";
    public static final Class CLASS_FROM_PARENT = Integer.class;
    public static final Class CLASS_FROM_CLASSLOADER1 = Object.class;
    public static final Class CLASS_FROM_CLASSLOADER2 = String.class;

    public static final String LIBRARY_NAME = "dummy.so";
    public static final String LIBRARY_FROM_PARENT = "parent.dummy.so";
    public static final String LIBRARY_FROM_CLASSLAODER1 = "classloader1.dummy.so";
    public static final String LIBRARY_FROM_CLASSLAODER2 = "classloader2.dummy.so";

    private final TestClassLoader parentClassLoader = new TestClassLoader();
    private final TestClassLoader classLoader1 = new TestClassLoader();
    private final TestClassLoader classLoader2 = new SubTestClassLoader();

    @Test
    public void usesParentOnlyLookup() throws ClassNotFoundException
    {
        parentClassLoader.addClass(CLASS_NAME, CLASS_FROM_PARENT);
        classLoader1.addClass(CLASS_NAME, CLASS_FROM_CLASSLOADER1);
        classLoader2.addClass(CLASS_NAME, CLASS_FROM_CLASSLOADER2);

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(lookupPolicy.getLookupStrategy(CLASS_NAME)).thenReturn(PARENT_ONLY);

        CompositeClassLoader compositeClassLoader = createCompositeClassLoader(lookupPolicy);

        Class<?> aClass = compositeClassLoader.loadClass(CLASS_NAME, true);
        assertThat(aClass, equalTo(CLASS_FROM_PARENT));
    }

    @Test(expected = ClassNotFoundException.class)
    public void usesParentOnlyLookupAndFails() throws ClassNotFoundException
    {
        classLoader1.addClass(CLASS_NAME, CLASS_FROM_CLASSLOADER1);
        classLoader2.addClass(CLASS_NAME, CLASS_FROM_CLASSLOADER2);

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(lookupPolicy.getLookupStrategy(CLASS_NAME)).thenReturn(PARENT_ONLY);
        CompositeClassLoader compositeClassLoader = createCompositeClassLoader(lookupPolicy);
        compositeClassLoader.loadClass(CLASS_NAME, true);
    }

    @Test
    public void usesParentFirstLookup() throws ClassNotFoundException
    {
        parentClassLoader.addClass(CLASS_NAME, CLASS_FROM_PARENT);
        classLoader1.addClass(CLASS_NAME, CLASS_FROM_CLASSLOADER1);
        classLoader2.addClass(CLASS_NAME, CLASS_FROM_CLASSLOADER2);

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(lookupPolicy.getLookupStrategy(CLASS_NAME)).thenReturn(PARENT_FIRST);
        CompositeClassLoader compositeClassLoader = createCompositeClassLoader(lookupPolicy);

        Class<?> aClass = compositeClassLoader.loadClass(CLASS_NAME, true);
        assertThat(aClass, equalTo(CLASS_FROM_PARENT));
    }

    @Test
    public void usesParentFirstThenChildLookup() throws ClassNotFoundException
    {
        classLoader1.addClass(CLASS_NAME, CLASS_FROM_CLASSLOADER1);
        classLoader2.addClass(CLASS_NAME, CLASS_FROM_CLASSLOADER2);

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(lookupPolicy.getLookupStrategy(CLASS_NAME)).thenReturn(PARENT_FIRST);
        CompositeClassLoader compositeClassLoader = createCompositeClassLoader(lookupPolicy);

        Class<?> aClass = compositeClassLoader.loadClass(CLASS_NAME, true);
        assertThat(aClass, equalTo(CLASS_FROM_CLASSLOADER1));
    }

    @Test(expected = ClassNotFoundException.class)
    public void  usesParentFirstAndChildLookupAndFails() throws ClassNotFoundException
    {
        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(lookupPolicy.getLookupStrategy(CLASS_NAME)).thenReturn(PARENT_FIRST);
        CompositeClassLoader compositeClassLoader = createCompositeClassLoader(lookupPolicy);

        compositeClassLoader.loadClass(CLASS_NAME, true);
    }

    @Test
    public void usesChildFirstLookupFromFirstClassLoader() throws ClassNotFoundException
    {
        parentClassLoader.addClass(CLASS_NAME, CLASS_FROM_PARENT);
        classLoader1.addClass(CLASS_NAME, CLASS_FROM_CLASSLOADER1);
        classLoader2.addClass(CLASS_NAME, CLASS_FROM_CLASSLOADER2);

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        CompositeClassLoader compositeClassLoader = createCompositeClassLoader(lookupPolicy);

        Class<?> aClass = compositeClassLoader.loadClass(CLASS_NAME, true);
        assertThat(aClass, equalTo(CLASS_FROM_CLASSLOADER1));
    }

    @Test
    public void usesChildFirstLookupFromSecondClasLoader() throws ClassNotFoundException
    {
        parentClassLoader.addClass(CLASS_NAME, CLASS_FROM_PARENT);
        classLoader2.addClass(CLASS_NAME, CLASS_FROM_CLASSLOADER2);

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        CompositeClassLoader compositeClassLoader = createCompositeClassLoader(lookupPolicy);

        Class<?> aClass = compositeClassLoader.loadClass(CLASS_NAME, true);
        assertThat(aClass, equalTo(CLASS_FROM_CLASSLOADER2));
    }

    @Test
    public void usesChildFirstThenParentLookup() throws ClassNotFoundException
    {
        parentClassLoader.addClass(CLASS_NAME, CLASS_FROM_PARENT);
        classLoader1.addClass(CLASS_NAME, CLASS_FROM_CLASSLOADER1);

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        CompositeClassLoader compositeClassLoader = createCompositeClassLoader(lookupPolicy);

        Class<?> aClass = compositeClassLoader.loadClass(CLASS_NAME, true);
        assertThat(aClass, equalTo(CLASS_FROM_CLASSLOADER1));
    }

    @Test(expected = ClassNotFoundException.class)
    public void usesChildFirstThenParentLookupAndFails() throws ClassNotFoundException
    {
        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        CompositeClassLoader compositeClassLoader = createCompositeClassLoader(lookupPolicy);

        compositeClassLoader.loadClass(CLASS_NAME, true);
    }

    @Test
    public void loadsLibraryFromFirstClassLoader() throws Exception
    {
        classLoader1.addLibrary(LIBRARY_NAME, LIBRARY_FROM_CLASSLAODER1);
        classLoader2.addLibrary(LIBRARY_NAME, LIBRARY_FROM_CLASSLAODER2);

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        CompositeClassLoader compositeClassLoader = createCompositeClassLoader(lookupPolicy);

        String library = compositeClassLoader.findLibrary(LIBRARY_NAME);

        assertThat(library, equalTo(LIBRARY_FROM_CLASSLAODER1));
    }

    @Test
    public void loadsLibraryFromSecondClassLoaderWhenIsNotDefinedOnTheFirstOne() throws Exception
    {
        classLoader2.addLibrary(LIBRARY_NAME, LIBRARY_FROM_CLASSLAODER2);

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        CompositeClassLoader compositeClassLoader = createCompositeClassLoader(lookupPolicy);

        String library = compositeClassLoader.findLibrary(LIBRARY_NAME);

        assertThat(library, equalTo(LIBRARY_FROM_CLASSLAODER2));
    }

    @Test
    public void returnsNullWhenLibraryIsNotDefinedInAnyClassLoader() throws Exception
    {
        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        CompositeClassLoader compositeClassLoader = createCompositeClassLoader(lookupPolicy);

        String library = compositeClassLoader.findLibrary(LIBRARY_NAME);

        assertThat(library, equalTo(null));
    }

    private CompositeClassLoader createCompositeClassLoader(ClassLoaderLookupPolicy lookupPolicy)
    {
        List<ClassLoader> classLoaders = new LinkedList<>();
        Collections.addAll(classLoaders, classLoader1, classLoader2);

        return new CompositeClassLoader(parentClassLoader, classLoaders, lookupPolicy);
    }

    // Used to ensure that the composite classloader is able to access
    // protected methods in subclasses by reflection
    public static class SubTestClassLoader extends TestClassLoader
    {

    }
}