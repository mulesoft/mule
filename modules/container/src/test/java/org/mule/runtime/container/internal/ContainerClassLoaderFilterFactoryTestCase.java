/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.container.internal;

import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderFilter;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ContainerClassLoaderFilterFactoryTestCase extends AbstractMuleTestCase {

  private ContainerClassLoaderFilterFactory factory = new ContainerClassLoaderFilterFactory();


  @Test
  public void acceptsExportedModulePackages() throws Exception {
    final List<MuleModule> muleModules = new ArrayList<>();
    muleModules.add(new TestModuleBuilder("module1").exportingPackages("org.foo1", "org.foo1.bar.")
        .exportingResources("META-INF/foo.txt", "META-INF/docs1/foo.txt").build());
    muleModules.add(new TestModuleBuilder("module2").exportingPackages("org.foo2")
        .exportingResources("META-INF/", "/META-INF/docs2").build());

    final ClassLoaderFilter classLoaderFilter = factory.create(ImmutableSet.of("com.sun"), muleModules);

    assertThat(classLoaderFilter.exportsClass("org.foo1.Foo"), is(true));
    assertThat(classLoaderFilter.exportsClass("org.foo1.bar.Bar"), is(true));
    assertThat(classLoaderFilter.exportsClass("org.foo2.Foo"), is(true));
    assertThat(classLoaderFilter.exportsClass("org.bar.Bar"), is(false));
    assertThat(classLoaderFilter.exportsClass("org.foo2.bar.Bar"), is(false));
    assertThat(classLoaderFilter.exportsPackage("org.foo1"), is(true));
    assertThat(classLoaderFilter.exportsPackage("org.foo1.bar"), is(true));
    assertThat(classLoaderFilter.exportsPackage("org.bar"), is(false));
    assertThat(classLoaderFilter.exportsPackage("org.foo2.bar"), is(false));
    assertThat(classLoaderFilter.exportsPackage("com.sun.xml"), is(true));
    assertThat(classLoaderFilter.exportsResource("META-INF/foo.txt"), is(true));
    assertThat(classLoaderFilter.exportsResource("META-INF/docs1/foo.txt"), is(true));
    assertThat(classLoaderFilter.exportsResource("META-INF/docs2/foo.txt"), is(true));
    assertThat(classLoaderFilter.exportsResource("/META-INF/docs2/foo.txt"), is(true));
    assertThat(classLoaderFilter.exportsResource("/foo.txt"), is(false));
  }

  @Test
  public void acceptsExportedSystemPackages() throws Exception {
    final List<MuleModule> muleModules = new ArrayList<>();
    final Set<String> bootPackages = singleton("org.foo1");

    final ClassLoaderFilter classLoaderFilter = factory.create(bootPackages, muleModules);

    assertThat(classLoaderFilter.exportsClass("org.foo1.Foo"), is(true));
    assertThat(classLoaderFilter.exportsClass("org.foo1.bar.Bar"), is(true));
    assertThat(classLoaderFilter.exportsClass("org.bar.Bar"), is(false));
  }

}
