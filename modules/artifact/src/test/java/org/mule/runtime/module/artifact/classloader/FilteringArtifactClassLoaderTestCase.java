/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

import org.junit.Test;

public class FilteringArtifactClassLoaderTestCase extends AbstractMuleTestCase
{

    public static final String CLASS_NAME = "java.lang.Object";
    public static final String RESOURCE_NAME = "dummy.txt";

    protected FilteringArtifactClassLoader filteringArtifactClassLoader;
    protected final ClassLoaderFilter filter = mock(ClassLoaderFilter.class);
    protected final ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);

    @Test(expected = ClassNotFoundException.class)
    public void throwClassNotFoundErrorWhenClassIsNotExported() throws ClassNotFoundException
    {
        when(filter.exportsClass(CLASS_NAME)).thenReturn(false);
        filteringArtifactClassLoader = doCreateClassLoader();

        filteringArtifactClassLoader.loadClass(CLASS_NAME);
    }

    protected FilteringArtifactClassLoader doCreateClassLoader()
    {
        return new FilteringArtifactClassLoader(artifactClassLoader, filter);
    }

    @Test
    public void loadsExportedClass() throws ClassNotFoundException
    {
        TestClassLoader classLoader = new TestClassLoader();
        Class expectedClass = this.getClass();
        classLoader.addClass(CLASS_NAME, expectedClass);

        when(filter.exportsClass(CLASS_NAME)).thenReturn(true);
        when(artifactClassLoader.getClassLoader()).thenReturn(classLoader);

        filteringArtifactClassLoader = doCreateClassLoader();
        Class<?> aClass = filteringArtifactClassLoader.loadClass(CLASS_NAME);
        assertThat(aClass, equalTo(expectedClass));
    }

    @Test
    public void filtersResourceWhenNotExported() throws ClassNotFoundException
    {
        when(filter.exportsClass(RESOURCE_NAME)).thenReturn(false);
        filteringArtifactClassLoader = doCreateClassLoader();

        URL resource = filteringArtifactClassLoader.getResource(RESOURCE_NAME);
        assertThat(resource, equalTo(null));
    }

    @Test
    public void loadsExportedResource() throws ClassNotFoundException, IOException
    {
        URL expectedResource = new URL("file:///app.txt");

        when(filter.exportsResource(RESOURCE_NAME)).thenReturn(true);
        when(artifactClassLoader.findResource(RESOURCE_NAME)).thenReturn(expectedResource);

        filteringArtifactClassLoader = doCreateClassLoader();

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

        filteringArtifactClassLoader = doCreateClassLoader();

        Enumeration<URL> resources = filteringArtifactClassLoader.getResources(RESOURCE_NAME);
        assertThat(resources, EnumerationMatcher.equalTo(Collections.EMPTY_LIST));
    }

    @Test
    public void getsExportedResources() throws Exception
    {
        URL resource = new URL("file:/app.txt");

        when(filter.exportsResource(RESOURCE_NAME)).thenReturn(true);
        when(artifactClassLoader.findResources(RESOURCE_NAME)).thenReturn(new EnumerationAdapter<>(Collections.singleton(resource)));

        filteringArtifactClassLoader = doCreateClassLoader();

        Enumeration<URL> resources = filteringArtifactClassLoader.getResources(RESOURCE_NAME);
        assertThat(resources, EnumerationMatcher.equalTo(Collections.singletonList(resource)));
    }

    @Test
    public void returnsCorrectClassLoader() throws Exception
    {
        filteringArtifactClassLoader = doCreateClassLoader();

        final ClassLoader classLoader = filteringArtifactClassLoader.getClassLoader();

        assertThat(classLoader, is(filteringArtifactClassLoader));
    }

    @Test
    public void doesNotDisposesFilteredClassLoader() throws Exception
    {
        filteringArtifactClassLoader = doCreateClassLoader();

        filteringArtifactClassLoader.dispose();

        verify(artifactClassLoader, never()).dispose();
    }
}
