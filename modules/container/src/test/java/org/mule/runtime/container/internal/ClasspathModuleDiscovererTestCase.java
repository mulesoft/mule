/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import static org.mule.runtime.container.api.MuleFoldersUtil.getModulesTempFolder;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.MODULE_PROPERTIES;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;

import static java.nio.file.Files.createTempFile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.jpms.api.MuleContainerModule;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class ClasspathModuleDiscovererTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder muleHome = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void discoversModule() throws Exception {
    runTest("invalidModule.properties", moduleDiscoverer -> {
      expectedException.expect(IllegalArgumentException.class);
      moduleDiscoverer.discover();
    });
  }

  @Test
  public void discoversModuleWithExportedJavaPackages() throws Exception {
    runTest("moduleJavaPackages.properties", moduleDiscoverer -> {
      List<MuleContainerModule> muleModules = moduleDiscoverer.discover();
      assertThat(muleModules, hasSize(1));
      MuleModule muleModule = (MuleModule) muleModules.get(0);
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
    runTest("moduleResourcePackages.properties", moduleDiscoverer -> {
      List<MuleContainerModule> muleModules = moduleDiscoverer.discover();
      assertThat(muleModules, hasSize(1));
      MuleModule muleModule = (MuleModule) muleModules.get(0);
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
    runTest("moduleJavaPrivilegedApi.properties", moduleDiscoverer -> {
      List<MuleContainerModule> muleModules = moduleDiscoverer.discover();
      assertThat(muleModules, hasSize(1));
      MuleModule muleModule = (MuleModule) muleModules.get(0);
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
    runTest("moduleExportedServices.properties", moduleDiscoverer -> {
      List<MuleContainerModule> muleModules = moduleDiscoverer.discover();
      assertThat(muleModules, hasSize(1));
      MuleModule muleModule = (MuleModule) muleModules.get(0);
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
    testWithMuleHome(MODULE_PROPERTIES, moduleDiscoverer -> {
      assertThat(getModulesTempFolder().list().length, is(0));

      File serviceFile = createTempFile(getModulesTempFolder().toPath(), "someService", "tmp").toFile();
      // Creating a new classpath module discoverer will delete any file in the modules temporary folder
      new ClasspathModuleDiscoverer(MODULE_PROPERTIES);

      assertThat(serviceFile.exists(), is(false));
      assertThat(getModulesTempFolder().list().length, is(0));
    });
  }

  @Test
  public void ignoresDuplicateModule() throws Exception {
    runTest("moduleJavaPackages.properties", moduleDiscoverer -> {
      List<MuleContainerModule> modules = moduleDiscoverer.discover();
      assertThat(modules.size(), equalTo(1));
    });
  }

  private void runTest(String modulePropertiesResource, CheckedConsumer<ClasspathModuleDiscoverer> testCallback)
      throws Exception {
    testWithMuleHome(modulePropertiesResource, testCallback);
    testWithExplicitFolder(modulePropertiesResource, testCallback);
  }

  private void testWithExplicitFolder(String modulePropertiesResource, CheckedConsumer<ClasspathModuleDiscoverer> testCallback)
      throws IOException {
    ClasspathModuleDiscoverer moduleDiscoverer = new ClasspathModuleDiscoverer(muleHome.newFolder(), modulePropertiesResource);
    testCallback.accept(moduleDiscoverer);
  }

  private void testWithMuleHome(String modulePropertiesResource, CheckedConsumer<ClasspathModuleDiscoverer> testCallback)
      throws Exception {
    testWithSystemProperty(MULE_HOME_DIRECTORY_PROPERTY, muleHome.getRoot().getAbsolutePath(), () -> {
      MuleFoldersUtil.getExecutionFolder().mkdir();
      ClasspathModuleDiscoverer moduleDiscoverer = new ClasspathModuleDiscoverer(modulePropertiesResource);
      testCallback.accept(moduleDiscoverer);
    });
  }

}
