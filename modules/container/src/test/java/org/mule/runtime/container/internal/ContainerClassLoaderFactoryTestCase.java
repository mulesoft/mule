/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ContainerClassLoaderFactoryTestCase extends AbstractMuleTestCase {

  @Test
  public void createsClassLoaderLookupPolicy() throws Exception {
    final ModuleRepository moduleRepository = mock(ModuleRepository.class);
    final ContainerClassLoaderFactory factory = new ContainerClassLoaderFactory(moduleRepository);
    final List<MuleModule> modules = new ArrayList<>();
    modules.add(new TestModuleBuilder("module1").exportingPackages("org.foo1", "org.foo1.bar").build());
    modules.add(new TestModuleBuilder("module2").exportingPackages("org.foo2").build());
    when(moduleRepository.getModules()).thenReturn(modules);

    final ArtifactClassLoader containerClassLoader = factory.createContainerClassLoader(this.getClass().getClassLoader());

    final ClassLoaderLookupPolicy classLoaderLookupPolicy = containerClassLoader.getClassLoaderLookupPolicy();
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.foo1.Foo"), instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.foo1.bar.Bar"), instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.foo2.Fo"), instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.foo2.bar.Bar"), sameInstance(CHILD_FIRST));
  }

  @Test
  public void getResourcesFromParent() throws Exception {
    final ContainerClassLoaderFactory factory = createClassLoaderExportingBootstrapProperties();

    final ArtifactClassLoader containerClassLoader = factory.createContainerClassLoader(this.getClass().getClassLoader());

    final Enumeration<URL> resources = containerClassLoader.getClassLoader().getResources(BOOTSTRAP_PROPERTIES);
    assertThat(resources.hasMoreElements(), is(true));

    Set<String> items = new HashSet<>();
    int size = 0;
    while (resources.hasMoreElements()) {
      final String url = resources.nextElement().toString();
      items.add(url);
      size++;
    }

    assertThat(size, equalTo(items.size()));
  }

  @Test
  public void doesNotFindAnyResource() throws Exception {
    final ContainerClassLoaderFactory factory = createClassLoaderExportingBootstrapProperties();

    final ArtifactClassLoader containerClassLoader = factory.createContainerClassLoader(this.getClass().getClassLoader());

    final URL resource = containerClassLoader.findResource(BOOTSTRAP_PROPERTIES);
    assertThat(resource, is(nullValue()));
  }

  @Test
  public void doesNotFindAnyResources() throws Exception {
    final ContainerClassLoaderFactory factory = createClassLoaderExportingBootstrapProperties();

    final ArtifactClassLoader containerClassLoader = factory.createContainerClassLoader(this.getClass().getClassLoader());

    final Enumeration<URL> resources = containerClassLoader.findResources(BOOTSTRAP_PROPERTIES);
    assertThat(resources.hasMoreElements(), is(false));
  }

  private ContainerClassLoaderFactory createClassLoaderExportingBootstrapProperties() {
    final ModuleRepository moduleRepository = mock(ModuleRepository.class);
    final ContainerClassLoaderFactory factory = new ContainerClassLoaderFactory(moduleRepository);
    final List<MuleModule> modules = new ArrayList<>();
    modules.add(new TestModuleBuilder("module1").exportingResources(BOOTSTRAP_PROPERTIES).build());
    when(moduleRepository.getModules()).thenReturn(modules);
    return factory;
  }
}
