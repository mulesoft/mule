/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static java.lang.System.lineSeparator;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_ONLY;

import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.module.artifact.classloader.exception.CompositeClassNotFoundException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class FineGrainedControlClassLoaderTestCase extends AbstractMuleTestCase
{

    public static final String TEST_CLASS_PACKAGE = "mypackage";
    public static final String TEST_CLASS_NAME = TEST_CLASS_PACKAGE + ".MyClass";
    public static final String EXPECTED_CHILD_MESSAGE = "Bye";
    public static final String EXPECTED_PARENT_MESSAGE = "Hello";

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void usesParentOnlyLookup() throws Exception
    {
        URLClassLoader parent = new URLClassLoader(new URL[] {getParentResource()}, Thread.currentThread().getContextClassLoader());

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(lookupPolicy.getLookupStrategy(TEST_CLASS_NAME)).thenReturn(PARENT_ONLY);
        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[] {getChildFileResource()}, parent, lookupPolicy);

        assertEquals(EXPECTED_PARENT_MESSAGE, invokeTestClassMethod(ext));
    }

    @Test
    public void usesParentOnlyLookupAndFails() throws Exception
    {
        ClassLoader parent = mock(ClassLoader.class);
        when(parent.loadClass(TEST_CLASS_NAME)).thenThrow(new ClassNotFoundException("ERROR"));

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(lookupPolicy.getLookupStrategy(TEST_CLASS_NAME)).thenReturn(PARENT_ONLY);

        expected.expect(CompositeClassNotFoundException.class);
        expected.expectMessage(startsWith("Cannot load class '" + TEST_CLASS_NAME + "': [ERROR" + lineSeparator() + "]"));

        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[] {getChildFileResource()}, parent, lookupPolicy);

        ext.loadClass(TEST_CLASS_NAME);
    }

    @Test
    public void usesParentFirstLookup() throws Exception
    {
        URLClassLoader parent = new URLClassLoader(new URL[] {getParentResource()}, Thread.currentThread().getContextClassLoader());

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(lookupPolicy.getLookupStrategy(TEST_CLASS_NAME)).thenReturn(PARENT_FIRST);

        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[] {getChildFileResource()}, parent, lookupPolicy);

        assertEquals(EXPECTED_PARENT_MESSAGE, invokeTestClassMethod(ext));
    }

    @Test
    public void usesParentFirstThenChildLookup() throws Exception
    {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(lookupPolicy.getLookupStrategy(TEST_CLASS_NAME)).thenReturn(PARENT_FIRST);

        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[] {getChildFileResource()}, parent, lookupPolicy);

        assertEquals(EXPECTED_CHILD_MESSAGE, invokeTestClassMethod(ext));
    }

    @Test
    public void usesParentFirstAndChildLookupAndFails() throws Exception
    {
        doClassNotFoundTest(PARENT_FIRST);
    }

    @Test
    public void usesChildFirstLookup() throws Exception
    {
        URLClassLoader parent = new URLClassLoader(new URL[] {getParentResource()}, Thread.currentThread().getContextClassLoader());

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(lookupPolicy.getLookupStrategy(TEST_CLASS_NAME)).thenReturn(CHILD_FIRST);

        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[] {getChildFileResource()}, parent, lookupPolicy);

        assertEquals(EXPECTED_CHILD_MESSAGE, invokeTestClassMethod(ext));
    }

    @Test
    public void usesChildFirstThenParentLookup() throws Exception
    {
        URLClassLoader parent = new URLClassLoader(new URL[] {getParentResource()}, Thread.currentThread().getContextClassLoader());

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(lookupPolicy.getLookupStrategy(TEST_CLASS_NAME)).thenReturn(PARENT_FIRST);

        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[0], parent, lookupPolicy);

        assertEquals(EXPECTED_PARENT_MESSAGE, invokeTestClassMethod(ext));
    }

    @Test
    public void usesChildFirstThenParentLookupAndFails() throws Exception
    {
        doClassNotFoundTest(CHILD_FIRST);
    }

    private void doClassNotFoundTest(ClassLoaderLookupStrategy lookupStrategy) throws Exception
    {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(lookupPolicy.getLookupStrategy(TEST_CLASS_NAME)).thenReturn(lookupStrategy);

        expected.expect(CompositeClassNotFoundException.class);
        expected.expectMessage(startsWith("Cannot load class '" + TEST_CLASS_NAME + "': ["));

        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[0], parent, lookupPolicy);

        invokeTestClassMethod(ext);
    }

    private URL getParentResource()
    {
        return ClassUtils.getResource("classloader-test-hello.jar", this.getClass());
    }

    private URL getChildFileResource()
    {
        return ClassUtils.getResource("classloader-test-bye.jar", this.getClass());
    }

    private String invokeTestClassMethod(ClassLoader loader) throws Exception
    {
        Class cls = loader.loadClass(TEST_CLASS_NAME);
        Method method = cls.getMethod("hi");
        return (String) method.invoke(cls.newInstance());
    }
}
