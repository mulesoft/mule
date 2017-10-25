/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.core.internal.util.EnumerationAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class ClasspathModuleDiscovererTestCase extends AbstractMuleTestCase {

  private final ClassLoader classLoader = mock(ClassLoader.class);
  private final ClasspathModuleDiscoverer moduleDiscoverer = new ClasspathModuleDiscoverer(classLoader);

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void discoversModule() throws Exception {
    List<URL> moduleProperties = new ArrayList();
    moduleProperties.add(getClass().getClassLoader().getResource("invalidModule.properties"));
    when(classLoader.getResources(ClasspathModuleDiscoverer.MODULE_PROPERTIES))
        .thenReturn(new EnumerationAdapter(moduleProperties));

    expectedException.expect(IllegalArgumentException.class);
    moduleDiscoverer.discover();
  }

  @Test
  public void discoversModuleWithExportedJavaPackages() throws Exception {
    List<URL> moduleProperties = new ArrayList();
    moduleProperties.add(getClass().getClassLoader().getResource("moduleJavaPackages.properties"));
    when(classLoader.getResources(ClasspathModuleDiscoverer.MODULE_PROPERTIES))
        .thenReturn(new EnumerationAdapter(moduleProperties));

    List<MuleModule> muleModules = moduleDiscoverer.discover();
    assertThat(muleModules, hasSize(1));
    MuleModule muleModule = muleModules.get(0);
    assertThat(muleModule.getName(), is("moduleJavaPackages"));
    assertThat(muleModule.getExportedPackages(), contains("org.foo", "org.bar"));
    assertThat(muleModule.getExportedPaths(), is(empty()));
    assertThat(muleModule.getPrivilegedExportedPackages(), is(empty()));
    assertThat(muleModule.getPrivilegedArtifacts(), is(empty()));
    assertThat(muleModule.getExportedServices(), is(empty()));
  }

  @Test
  public void discoversModuleWithExportedResourcePackages() throws Exception {
    List<URL> moduleProperties = new ArrayList();
    moduleProperties.add(getClass().getClassLoader().getResource("moduleResourcePackages.properties"));
    when(classLoader.getResources(ClasspathModuleDiscoverer.MODULE_PROPERTIES))
        .thenReturn(new EnumerationAdapter(moduleProperties));

    List<MuleModule> muleModules = moduleDiscoverer.discover();
    assertThat(muleModules, hasSize(1));
    MuleModule muleModule = muleModules.get(0);
    assertThat(muleModule.getName(), is("moduleResourcePackages"));
    assertThat(muleModule.getExportedPackages(), is(empty()));
    assertThat(muleModule.getExportedPaths(), containsInAnyOrder("META-INF/module.xsd", "README.txt"));
    assertThat(muleModule.getPrivilegedExportedPackages(), is(empty()));
    assertThat(muleModule.getPrivilegedArtifacts(), is(empty()));
    assertThat(muleModule.getExportedServices(), is(empty()));
  }

  @Test
  public void discoversModuleWithExportedPrivilegedApi() throws Exception {
    List<URL> moduleProperties = new ArrayList();
    moduleProperties.add(getClass().getClassLoader().getResource("moduleJavaPrivilegedApi.properties"));
    when(classLoader.getResources(ClasspathModuleDiscoverer.MODULE_PROPERTIES))
        .thenReturn(new EnumerationAdapter(moduleProperties));

    List<MuleModule> muleModules = moduleDiscoverer.discover();
    assertThat(muleModules, hasSize(1));
    MuleModule muleModule = muleModules.get(0);
    assertThat(muleModule.getName(), is("moduleJavaPrivilegedApi"));
    assertThat(muleModule.getExportedPackages(), is(empty()));
    assertThat(muleModule.getExportedPaths(), is(empty()));
    assertThat(muleModule.getPrivilegedExportedPackages(), contains("org.foo", "org.bar"));
    assertThat(muleModule.getPrivilegedArtifacts(), contains("privilegedArtifact1", "privilegedArtifact2"));
    assertThat(muleModule.getExportedServices(), is(empty()));
  }

  @Test
  public void discoversModuleWithExportedServices() throws Exception {
    List<URL> moduleProperties = new ArrayList();
    moduleProperties.add(getClass().getClassLoader().getResource("moduleExportedServices.properties"));
    when(classLoader.getResources(ClasspathModuleDiscoverer.MODULE_PROPERTIES))
        .thenReturn(new EnumerationAdapter(moduleProperties));

    List<MuleModule> muleModules = moduleDiscoverer.discover();
    assertThat(muleModules, hasSize(1));
    MuleModule muleModule = muleModules.get(0);
    assertThat(muleModule.getName(), is("moduleExportedServices"));
    assertThat(muleModule.getExportedPackages(), is(empty()));
    assertThat(muleModule.getExportedPaths(), is(empty()));
    assertThat(muleModule.getPrivilegedExportedPackages(), is(empty()));
    assertThat(muleModule.getPrivilegedArtifacts(), is(empty()));
    assertThat(muleModule.getExportedServices().size(), equalTo(3));
    assertThat(muleModule.getExportedServices().get(0).getServiceInterface(), equalTo("org.foo.ServiceInterface"));
    assertThat(muleModule.getExportedServices().get(1).getServiceInterface(), equalTo("org.foo.ServiceInterface"));
    assertThat(muleModule.getExportedServices().get(2).getServiceInterface(), equalTo("org.bar.ServiceInterface"));
  }

  @Test
  public void ignoresDuplicateModule() throws Exception {
    List<URL> moduleProperties = new ArrayList();
    moduleProperties.add(getClass().getClassLoader().getResource("moduleJavaPackages.properties"));
    when(classLoader.getResources(ClasspathModuleDiscoverer.MODULE_PROPERTIES))
        .thenReturn(new EnumerationAdapter(moduleProperties));

    List<MuleModule> modules = moduleDiscoverer.discover();
    assertThat(modules.size(), equalTo(1));
  }
}
