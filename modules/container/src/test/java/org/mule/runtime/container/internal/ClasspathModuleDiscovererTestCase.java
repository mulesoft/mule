/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static java.nio.file.Files.createTempFile;
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
import static org.mule.runtime.container.api.MuleFoldersUtil.getModulesTempFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.internal.util.EnumerationAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class ClasspathModuleDiscovererTestCase extends AbstractMuleTestCase {

  private final ClassLoader classLoader = mock(ClassLoader.class);

  @Rule
  public TemporaryFolder muleHome = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void discoversModule() throws Exception {
    runTest(moduleDiscoverer -> {
      List<URL> moduleProperties = new ArrayList();
      moduleProperties.add(getClass().getClassLoader().getResource("invalidModule.properties"));
      when(classLoader.getResources(ClasspathModuleDiscoverer.MODULE_PROPERTIES))
          .thenReturn(new EnumerationAdapter(moduleProperties));

      expectedException.expect(IllegalArgumentException.class);
      moduleDiscoverer.discover();
    });
  }

  @Test
  public void discoversModuleWithExportedJavaPackages() throws Exception {
    runTest(moduleDiscoverer -> {
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
    });
  }

  @Test
  public void discoversModuleWithExportedResourcePackages() throws Exception {
    runTest(moduleDiscoverer -> {
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
    });
  }

  @Test
  public void discoversModuleWithExportedPrivilegedApi() throws Exception {
    runTest(moduleDiscoverer -> {
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
    });
  }

  @Test
  public void discoversModuleWithExportedServices() throws Exception {
    runTest(moduleDiscoverer -> {
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
    });
  }

  @Test
  public void shouldDeleteTemporaryModuleExportedServicesFilesWhenCreated() throws Exception {
    testWithMuleHome(moduleDiscoverer -> {
      assertThat(getModulesTempFolder().list().length, is(0));

      File serviceFile = createTempFile(getModulesTempFolder().toPath(), "someService", "tmp").toFile();
      // Creating a new classpath module discoverer will delete any file in the modules temporary folder
      new ClasspathModuleDiscoverer(classLoader);

      assertThat(serviceFile.exists(), is(false));
      assertThat(getModulesTempFolder().list().length, is(0));
    });
  }

  @Test
  public void ignoresDuplicateModule() throws Exception {
    runTest(moduleDiscoverer -> {
      List<URL> moduleProperties = new ArrayList();
      moduleProperties.add(getClass().getClassLoader().getResource("moduleJavaPackages.properties"));
      when(classLoader.getResources(ClasspathModuleDiscoverer.MODULE_PROPERTIES))
          .thenReturn(new EnumerationAdapter(moduleProperties));

      List<MuleModule> modules = moduleDiscoverer.discover();
      assertThat(modules.size(), equalTo(1));
    });
  }

  private void runTest(CheckedConsumer<ClasspathModuleDiscoverer> testCallback) throws Exception {
    testWithMuleHome(testCallback);
    testWithExplicitFolder(testCallback);
  }

  private void testWithExplicitFolder(CheckedConsumer<ClasspathModuleDiscoverer> testCallback) throws IOException {
    ClasspathModuleDiscoverer moduleDiscoverer = new ClasspathModuleDiscoverer(classLoader, muleHome.newFolder());
    testCallback.accept(moduleDiscoverer);
  }

  private void testWithMuleHome(CheckedConsumer<ClasspathModuleDiscoverer> testCallback) throws Exception {
    testWithSystemProperty(MULE_HOME_DIRECTORY_PROPERTY, muleHome.getRoot().getAbsolutePath(), () -> {
      MuleFoldersUtil.getExecutionFolder().mkdir();
      ClasspathModuleDiscoverer moduleDiscoverer = new ClasspathModuleDiscoverer(classLoader);
      testCallback.accept(moduleDiscoverer);
    });
  }

}
