/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.application;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppLibFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.internal.domain.MuleSharedDomainClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

@SmallTest
@RunWith(PowerMockRunner.class)
@PrepareForTest(MuleArtifactClassLoader.class)
public class MuleApplicationClassLoaderTestCase extends AbstractMuleTestCase {

  private static final String RESOURCE_IN_CLASSES_AND_JAR = "test-resource-1.txt";
  private static final String RESOURCE_JUST_IN_JAR = "test-resource-2.txt";
  private static final String RESOURCE_JUST_IN_CLASSES = "test-resource-3.txt";
  private static final String RESOURCE_JUST_IN_DOMAIN = "test-resource-4.txt";

  private static final String DOMAIN_NAME = "test-domain";
  private static final String APP_NAME = "test-app";

  @Rule
  public TemporaryFolder tempMuleHome = new TemporaryFolder();

  private String previousMuleHome;
  private MuleSharedDomainClassLoader domainCL;
  private MuleApplicationClassLoader appCL;
  private File domainDir;
  private File appFolder;
  private File jarFile;

  @Before
  public void createAppClassLoader() throws Exception {
    // Create directories structure
    previousMuleHome = System.setProperty(MULE_HOME_DIRECTORY_PROPERTY, tempMuleHome.getRoot().getAbsolutePath());

    final List<URL> urls = new LinkedList<>();

    appFolder = getAppFolder(APP_NAME);
    assertThat(appFolder.mkdirs(), is(true));
    // Add isolated resources in classes dir
    FileUtils.stringToFile(new File(appFolder, RESOURCE_IN_CLASSES_AND_JAR).getAbsolutePath(), "Some text");
    FileUtils.stringToFile(new File(appFolder, RESOURCE_JUST_IN_CLASSES).getAbsolutePath(), "Some text");
    urls.add(appFolder.toURI().toURL());

    // Add jar file with resources in lib dir
    File libDir = getAppLibFolder(APP_NAME);
    assertThat(libDir.mkdirs(), is(true));
    URL resourceSrcJarFile = currentThread().getContextClassLoader().getResource("test-jar-with-resources.jar");
    assertNotNull(resourceSrcJarFile);
    File srcJarFile = new File(resourceSrcJarFile.toURI());
    jarFile = new File(libDir, "test-jar-with-resources.jar");
    FileUtils.copyFile(srcJarFile, jarFile, false);
    urls.add(jarFile.toURI().toURL());

    // Add isolated resources in domain dir
    domainDir = MuleFoldersUtil.getDomainFolder(DOMAIN_NAME);
    assertThat(domainDir.mkdirs(), is(true));
    FileUtils.stringToFile(new File(domainDir, RESOURCE_JUST_IN_DOMAIN).getAbsolutePath(), "Some text");

    mockStatic(MuleArtifactClassLoader.class);

    // Create app class loader
    domainCL =
        new MuleSharedDomainClassLoader(new DomainDescriptor(DOMAIN_NAME), currentThread().getContextClassLoader(),
                                        mock(ClassLoaderLookupPolicy.class), emptyList(), emptyList());

    final ApplicationDescriptor applicationDescriptor = new ApplicationDescriptor(APP_NAME);
    ClassLoaderModel classLoaderModel = new ClassLoaderModel.ClassLoaderModelBuilder(applicationDescriptor.getClassLoaderModel())
        .containing(getAppFolder(APP_NAME).toURI().toURL()).build();
    applicationDescriptor.setClassLoaderModel(classLoaderModel);
    appCL = new MuleApplicationClassLoader(APP_NAME, applicationDescriptor, domainCL, null, urls,
                                           mock(ClassLoaderLookupPolicy.class), emptyList());
  }

  @After
  public void cleanUp() {
    if (previousMuleHome != null) {
      System.setProperty(MULE_HOME_DIRECTORY_PROPERTY, previousMuleHome);
    }
    FileUtils.deleteTree(tempMuleHome.getRoot());
  }

  @Test
  public void loadLocalResourcesOnlyFromExpectedDirectories() throws Exception {
    // Ensure all resources can be loaded from their respective locations using findResource
    assertLoadedFromClassesDir(appCL.findResource(RESOURCE_IN_CLASSES_AND_JAR));
    assertLoadedFromJarFile(appCL.findResource(RESOURCE_JUST_IN_JAR));
    assertLoadedFromClassesDir(appCL.findResource(RESOURCE_JUST_IN_CLASSES));
    assertNotLoaded(appCL.findResource(RESOURCE_JUST_IN_DOMAIN));
    assertLoadedFromDomainDir(domainCL.findResource(RESOURCE_JUST_IN_DOMAIN));

    // Ensure app resources are only loaded from classes directory
    assertLoadedFromClassesDir(appCL.findLocalResource(RESOURCE_IN_CLASSES_AND_JAR));
    assertLoadedFromClassesDir(appCL.findLocalResource(RESOURCE_JUST_IN_CLASSES));
    assertNotLoaded(appCL.findLocalResource(RESOURCE_JUST_IN_JAR));
    assertNotLoaded(appCL.findLocalResource(RESOURCE_JUST_IN_DOMAIN));
  }

  private void assertLoadedFromClassesDir(URL resource) throws URISyntaxException {
    assertNotNull(resource);
    assertEquals("file", resource.getProtocol());
    assertTrue(resource.toURI().toString().contains(appFolder.toURI().toString()));
  }

  private void assertLoadedFromJarFile(URL resource) throws URISyntaxException {
    assertNotNull(resource);
    assertEquals("jar", resource.getProtocol());
    assertTrue(resource.toURI().toString().contains(jarFile.toURI().toString()));
  }

  private void assertLoadedFromDomainDir(URL resource) throws URISyntaxException {
    assertNotNull(resource);
    assertEquals("file", resource.getProtocol());
    assertTrue(resource.toURI().toString().contains(domainDir.toURI().toString()));
  }

  private void assertNotLoaded(URL resource) {
    assertNull(resource);
  }
}
