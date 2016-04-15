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
import org.mule.module.artifact.classloader.ArtifactClassLoader;
import org.mule.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.module.launcher.ServerPluginClassLoaderManager;
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

    private final ArtifactClassLoaderFactory applicationClassLoaderFactory = mock(ArtifactClassLoaderFactory.class, RETURNS_DEEP_STUBS.get());
    private final ServerPluginClassLoaderManager serverPluginClassLoaderManager = mock(ServerPluginClassLoaderManager.class);
    private final CompositeApplicationClassLoaderFactory pluginAwareClassLoaderFactory = new CompositeApplicationClassLoaderFactory(applicationClassLoaderFactory, serverPluginClassLoaderManager);
    private final ApplicationDescriptor appDescriptor = new ApplicationDescriptor();

    @Test
    public void createsDefaultApplicationClassLoaderWhenNoPluginInstalled() throws Exception
    {
        ClassLoader expectedClassLoader = Thread.currentThread().getContextClassLoader();
        when(applicationClassLoaderFactory.create(null, appDescriptor).getClassLoader()).thenReturn(expectedClassLoader);
        when(serverPluginClassLoaderManager.getPluginClassLoaders()).thenReturn(Collections.EMPTY_LIST);

        ClassLoader appClassLoader = pluginAwareClassLoaderFactory.create(null, appDescriptor).getClassLoader();

        assertThat(appClassLoader, equalTo(expectedClassLoader));
    }

    @Test
    public void createsCompositeWhenPluginsInstalled() throws Exception
    {
        TestClassLoader appClassLoader = new TestClassLoader();
        when(applicationClassLoaderFactory.create(null, appDescriptor).getClassLoader()).thenReturn(appClassLoader);

        final List<ArtifactClassLoader> artifactClassLoaders = new LinkedList<>();
        TestClassLoader pluginClassLoader = new TestClassLoader();
        ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);
        when(artifactClassLoader.getClassLoader()).thenReturn(pluginClassLoader);
        artifactClassLoaders.add(artifactClassLoader);
        when(serverPluginClassLoaderManager.getPluginClassLoaders()).thenReturn(artifactClassLoaders);

        ClassLoader createdClassLoader = pluginAwareClassLoaderFactory.create(null, appDescriptor).getClassLoader();

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
