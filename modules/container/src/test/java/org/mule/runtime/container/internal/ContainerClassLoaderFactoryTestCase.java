/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.CLASSLOADER_CONTAINER_JPMS_MODULE_LAYER;
import static org.mule.runtime.core.internal.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;

import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import io.qameta.allure.Issue;

public class ContainerClassLoaderFactoryTestCase extends AbstractMuleTestCase {

  @Test
  public void createsClassLoaderLookupPolicy() {
    final ModuleRepository moduleRepository = mock(ModuleRepository.class);
    final ContainerClassLoaderFactory factory = new ContainerClassLoaderFactory(moduleRepository);
    final List<MuleModule> modules = new ArrayList<>();
    modules.add(new TestModuleBuilder("module1").exportingPackages("org.foo1", "org.foo1.bar").build());
    modules.add(new TestModuleBuilder("module2").exportingPackages("org.foo2").build());
    when(moduleRepository.getModules()).thenReturn(modules);

    final ArtifactClassLoader containerClassLoader =
        factory.createContainerClassLoader(this.getClass().getClassLoader()).getContainerClassLoader();

    final ClassLoaderLookupPolicy classLoaderLookupPolicy = containerClassLoader.getClassLoaderLookupPolicy();
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.foo1.Foo"), instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.foo1.bar.Bar"), instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.foo2.Fo"), instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.foo2.bar.Bar"), sameInstance(CHILD_FIRST));
  }

  @Test
  public void considersJreExtensions() {
    final ModuleRepository moduleRepository = mock(ModuleRepository.class);
    final ContainerClassLoaderFactory factory = new ContainerClassLoaderFactory(moduleRepository);
    final List<MuleModule> modules = new ArrayList<>();
    modules.add(new TestModuleBuilder("jre1")
        .exportingPackages("org.w3c.dom.test", "javax.test", "java.applet", "org.w3c.dom", "org.omg.test").build());
    modules.add(new TestModuleBuilder("jre2")
        .exportingPackages("org.ietf.jgss", "org.ietf.jgss.test", "org.xml.sax", "org.xml.sax.test").build());
    when(moduleRepository.getModules()).thenReturn(modules);

    final ArtifactClassLoader containerClassLoader =
        factory.createContainerClassLoader(this.getClass().getClassLoader()).getContainerClassLoader();

    final ClassLoaderLookupPolicy classLoaderLookupPolicy = containerClassLoader.getClassLoaderLookupPolicy();
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.w3c.dom.Foo"), sameInstance(PARENT_FIRST));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.w3c.dom.test.Foo"), sameInstance(PARENT_FIRST));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("javax.test.Bar"), sameInstance(PARENT_FIRST));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.omg.test.Bar"), sameInstance(PARENT_FIRST));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.ietf.jgss.Bar"), sameInstance(PARENT_FIRST));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.ietf.jgss.test.Bar"), sameInstance(PARENT_FIRST));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.xml.sax.Foo"), sameInstance(PARENT_FIRST));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.xml.sax.test.Bar"), sameInstance(PARENT_FIRST));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("java.applet.Applet"),
               instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.foo2.bar.Bar"), sameInstance(CHILD_FIRST));
  }

  @Test
  public void getResourcesFromParent() throws Exception {
    final ContainerClassLoaderFactory factory = createClassLoaderExportingBootstrapProperties();

    final ArtifactClassLoader containerClassLoader =
        factory.createContainerClassLoader(this.getClass().getClassLoader()).getContainerClassLoader();

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

    final ArtifactClassLoader containerClassLoader =
        factory.createContainerClassLoader(this.getClass().getClassLoader()).getContainerClassLoader();

    final URL resource = containerClassLoader.findResource(BOOTSTRAP_PROPERTIES);
    assertThat(resource, is(nullValue()));
  }

  @Test
  public void doesNotFindAnyResources() throws Exception {
    final ContainerClassLoaderFactory factory = createClassLoaderExportingBootstrapProperties();

    final ArtifactClassLoader containerClassLoader =
        factory.createContainerClassLoader(this.getClass().getClassLoader()).getContainerClassLoader();

    final Enumeration<URL> resources = containerClassLoader.findResources(BOOTSTRAP_PROPERTIES);
    assertThat(resources.hasMoreElements(), is(false));
  }

  @Test
  public void considersSdkApiUsagesInExtensions() {
    final ModuleRepository moduleRepository = mock(ModuleRepository.class);
    final ContainerClassLoaderFactory factory = new ContainerClassLoaderFactory(moduleRepository);
    final List<MuleModule> modules = new ArrayList<>();
    modules.add(new TestModuleBuilder("sdk-module")
        .exportingPackages("org.mule.sdk.api.connectivity", "org.mule.sdk.compatibility.api.helper", "org.omg.test").build());
    modules.add(new TestModuleBuilder("other-sdk-module")
        .exportingPackages("org.mule.sdk.api.runtime.streaming", "org.mule.extension.validation.api.condition",
                           "org.xml.sax.test")
        .build());
    when(moduleRepository.getModules()).thenReturn(modules);

    final ArtifactClassLoader containerClassLoader =
        factory.createContainerClassLoader(this.getClass().getClassLoader()).getContainerClassLoader();

    final ClassLoaderLookupPolicy classLoaderLookupPolicy = containerClassLoader.getClassLoaderLookupPolicy();
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.mule.sdk.api.connectivity.Foo"), sameInstance(PARENT_FIRST));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.mule.sdk.compatibility.api.helper.Something"),
               sameInstance(PARENT_FIRST));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.omg.test.Bar"), sameInstance(PARENT_FIRST));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.mule.sdk.api.runtime.streaming.SomeStreaming"),
               sameInstance(PARENT_FIRST));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.mule.extension.validation.api.condition.SomeCondition"),
               instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(classLoaderLookupPolicy.getClassLookupStrategy("org.xml.sax.test.Bar"), sameInstance(PARENT_FIRST));
  }

  @Test
  @Issue("W-13951850")
  public void actualContainerClassLoaderNotAccessibleThroughParents() {
    assumeTrue(isJavaVersionAtLeast(JAVA_17));

    final URLClassLoader containerParentClassLoader = new URLClassLoader(new URL[0]);
    final ArtifactClassLoader containerClassLoader = createContainerClassLoader(containerParentClassLoader);
    assertThat(containerClassLoader.getClassLoader().getParent(), is(not(containerParentClassLoader)));
  }

  @Test
  @Issue("W-13951850")
  public void actualContainerClassLoaderAccessibleThroughParentsFallback() {
    assumeTrue(isJavaVersionAtLeast(JAVA_17));
    setProperty(CLASSLOADER_CONTAINER_JPMS_MODULE_LAYER, "false");

    try {
      final URLClassLoader containerParentClassLoader = new URLClassLoader(new URL[0]);
      final ArtifactClassLoader containerClassLoader = createContainerClassLoader(containerParentClassLoader);
      assertThat(containerClassLoader.getClassLoader().getParent(), is(containerParentClassLoader));
    } finally {
      clearProperty(CLASSLOADER_CONTAINER_JPMS_MODULE_LAYER);
    }
  }

  private ArtifactClassLoader createContainerClassLoader(final URLClassLoader containerParentClassLoader) {
    final ModuleRepository moduleRepository = mock(ModuleRepository.class);
    final ContainerClassLoaderFactory factory = new ContainerClassLoaderFactory(moduleRepository);
    final ArtifactClassLoader containerClassLoader =
        factory.createContainerClassLoader(containerParentClassLoader).getContainerClassLoader();
    return containerClassLoader;
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
