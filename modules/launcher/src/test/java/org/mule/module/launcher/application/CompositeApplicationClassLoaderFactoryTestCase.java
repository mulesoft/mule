/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.module.launcher.PluginClassLoaderManager;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

@SmallTest
public class CompositeApplicationClassLoaderFactoryTestCase extends AbstractMuleTestCase
{

    private final ApplicationClassLoaderFactory applicationClassLoaderFactory = mock(ApplicationClassLoaderFactory.class, RETURNS_DEEP_STUBS.get());
    private final PluginClassLoaderManager pluginClassLoaderManager = mock(PluginClassLoaderManager.class);
    private final CompositeApplicationClassLoaderFactory pluginAwareClassLoaderFactory = new CompositeApplicationClassLoaderFactory(applicationClassLoaderFactory, pluginClassLoaderManager);
    private final ApplicationDescriptor appDescriptor = new ApplicationDescriptor();

    @Test
    public void createsDefaultApplicationClassLoaderWhenNoPluginInstalled() throws Exception
    {
        ClassLoader expectedClassLoader = Thread.currentThread().getContextClassLoader();
        when(applicationClassLoaderFactory.create(appDescriptor).getClassLoader()).thenReturn(expectedClassLoader);
        when(pluginClassLoaderManager.getPluginClassLoaders()).thenReturn(Collections.EMPTY_LIST);

        ClassLoader appClassLoader = pluginAwareClassLoaderFactory.create(appDescriptor).getClassLoader();

        assertThat(appClassLoader, equalTo(expectedClassLoader));
    }

    @Test
    public void createsCompositeWhenPluginsInstalled() throws Exception
    {
        TestClassLoader appClassLoader = new TestClassLoader();
        when(applicationClassLoaderFactory.create(appDescriptor).getClassLoader()).thenReturn(appClassLoader);

        TestClassLoader pluginClassLoader = new TestClassLoader();
        List<ClassLoader> pluginClassLoaders = new LinkedList<ClassLoader>();
        pluginClassLoaders.add(pluginClassLoader);
        when(pluginClassLoaderManager.getPluginClassLoaders()).thenReturn(pluginClassLoaders);

        ClassLoader createdClassLoader = pluginAwareClassLoaderFactory.create(appDescriptor).getClassLoader();

        createdClassLoader.getResource("foo");
        assertThat(appClassLoader.loadedResource, equalTo(true));
        assertThat(pluginClassLoader.loadedResource, equalTo(true));
    }

    public static class TestClassLoader extends ClassLoader
    {

        private boolean loadedResource;

        @Override
        public URL getResource(String s)
        {
            loadedResource = true;
            return null;
        }
    }
}
