/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_ONLY;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ContainerClassLoaderFactoryTestCase
{

    @Test
    public void createsClassLoaderLookupPolicy() throws Exception
    {
        final ContainerClassLoaderFactory factory = new ContainerClassLoaderFactory();
        final ModuleDiscoverer moduleDiscoverer = mock(ModuleDiscoverer.class);
        final List<MuleModule> modules = new ArrayList<>();
        modules.add(new TestModuleBuilder("module1").exportingPackages("org.foo1", "org.foo1.bar").build());
        modules.add(new TestModuleBuilder("module2").exportingPackages("org.foo2").build());
        when(moduleDiscoverer.discover()).thenReturn(modules);
        factory.setModuleDiscoverer(moduleDiscoverer);

        final ArtifactClassLoader containerClassLoader = factory.createContainerClassLoader(this.getClass().getClassLoader());

        final ClassLoaderLookupPolicy classLoaderLookupPolicy = containerClassLoader.getClassLoaderLookupPolicy();
        assertThat(classLoaderLookupPolicy.getLookupStrategy("org.foo1.Foo"), is(PARENT_ONLY));
        assertThat(classLoaderLookupPolicy.getLookupStrategy("org.foo1.bar.Bar"), is(PARENT_ONLY));
        assertThat(classLoaderLookupPolicy.getLookupStrategy("org.foo2.Fo"), is(PARENT_ONLY));
        assertThat(classLoaderLookupPolicy.getLookupStrategy("org.foo2.bar.Bar"), is(CHILD_FIRST));
    }
}