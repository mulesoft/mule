/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

import org.junit.Test;

public class FilteringArtifactClassLoaderTestCase extends AbstractMuleTestCase
{

    public static final String CLASS_NAME = "java.lang.Object";
    public static final String RESOURCE_NAME = "dummy.txt";
    public static final String PLUGIN_NAME = "DUMMY_PLUGIN";

    private FilteringArtifactClassLoader filteringArtifactClassLoader;
    private final ClassLoaderFilter filter = mock(ClassLoaderFilter.class);
    private final ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);

    @Test(expected = ClassNotFoundException.class)
    public void throwClassNotFoundErrorWhenClassIsNotExported() throws ClassNotFoundException
    {
        when(filter.exportsClass(CLASS_NAME)).thenReturn(false);
        filteringArtifactClassLoader = new FilteringArtifactClassLoader(PLUGIN_NAME, null, filter);

        filteringArtifactClassLoader.loadClass(CLASS_NAME);
    }

    @Test
    public void loadsExportedClass() throws ClassNotFoundException
    {
        TestClassLoader classLoader = new TestClassLoader();
        Class expectedClass = this.getClass();
        classLoader.addClass(CLASS_NAME, expectedClass);

        when(filter.exportsClass(CLASS_NAME)).thenReturn(true);
        when(artifactClassLoader.getClassLoader()).thenReturn(classLoader);

        filteringArtifactClassLoader = new FilteringArtifactClassLoader(PLUGIN_NAME, artifactClassLoader, filter);
        Class<?> aClass = filteringArtifactClassLoader.loadClass(CLASS_NAME);
        assertThat(aClass, equalTo(expectedClass));
    }

    @Test
    public void filtersResourceWhenNotExported() throws ClassNotFoundException
    {
        when(filter.exportsClass(RESOURCE_NAME)).thenReturn(false);
        filteringArtifactClassLoader = new FilteringArtifactClassLoader(PLUGIN_NAME, null, filter);

        URL resource = filteringArtifactClassLoader.getResource(RESOURCE_NAME);
        assertThat(resource, equalTo(null));
    }

    @Test
    public void loadsExportedResource() throws ClassNotFoundException, MalformedURLException
    {
        TestClassLoader classLoader = new TestClassLoader();
        URL expectedResource = new URL("file:///app.txt");
        classLoader.addResource(RESOURCE_NAME, expectedResource);

        when(filter.exportsResource(RESOURCE_NAME)).thenReturn(true);
        when(artifactClassLoader.getClassLoader()).thenReturn(classLoader);

        filteringArtifactClassLoader = new FilteringArtifactClassLoader(PLUGIN_NAME, artifactClassLoader, filter);

        URL resource = filteringArtifactClassLoader.getResource(RESOURCE_NAME);
        assertThat(resource, equalTo(expectedResource));
    }

    @Test
    public void filtersResources() throws Exception
    {
        TestClassLoader classLoader = new TestClassLoader();
        URL blockedResource = new URL("file:///app.txt");
        classLoader.addResource(RESOURCE_NAME, blockedResource);

        when(filter.exportsResource(RESOURCE_NAME)).thenReturn(false);
        when(artifactClassLoader.getClassLoader()).thenReturn(classLoader);

        filteringArtifactClassLoader = new FilteringArtifactClassLoader(PLUGIN_NAME, artifactClassLoader, filter);

        Enumeration<URL> resources = filteringArtifactClassLoader.getResources(RESOURCE_NAME);
        assertThat(resources, EnumerationMatcher.equalTo(Collections.EMPTY_LIST));
    }

    @Test
    public void getsExportedResources() throws Exception
    {
        TestClassLoader classLoader = new TestClassLoader();
        URL resource = new URL("file:/app.txt");
        classLoader.addResource(RESOURCE_NAME, resource);

        when(filter.exportsResource(RESOURCE_NAME)).thenReturn(true);
        when(artifactClassLoader.getClassLoader()).thenReturn(classLoader);

        filteringArtifactClassLoader = new FilteringArtifactClassLoader(PLUGIN_NAME, artifactClassLoader, filter);

        Enumeration<URL> resources = filteringArtifactClassLoader.getResources(RESOURCE_NAME);
        assertThat(resources, EnumerationMatcher.equalTo(Collections.singletonList(resource)));
    }

    @Test
    public void returnsCorrectClassLoader() throws Exception
    {
        filteringArtifactClassLoader = new FilteringArtifactClassLoader(PLUGIN_NAME, artifactClassLoader, filter);

        final ClassLoader classLoader = filteringArtifactClassLoader.getClassLoader();

        assertThat(classLoader, is(filteringArtifactClassLoader));
    }

    @Test
    public void doesNotDisposesFilteredClassLoader() throws Exception
    {
        filteringArtifactClassLoader = new FilteringArtifactClassLoader(PLUGIN_NAME, artifactClassLoader, filter);

        filteringArtifactClassLoader.dispose();

        verify(artifactClassLoader, never()).dispose();
    }
}
