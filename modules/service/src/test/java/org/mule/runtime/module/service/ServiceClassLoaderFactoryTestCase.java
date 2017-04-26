/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service;

import static junit.framework.TestCase.fail;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ServiceClassLoaderFactoryTestCase extends AbstractMuleTestCase {

  private static final String SERVICE_ID = "service/serviceId";

  private final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
  @Rule
  public TemporaryFolder serviceFolder = new TemporaryFolder();
  private ServiceClassLoaderFactory factory = new ServiceClassLoaderFactory();
  private ServiceDescriptor descriptor;

  @Before
  public void setUp() throws Exception {
    descriptor = new ServiceDescriptor("testService");
    descriptor.setRootFolder(serviceFolder.getRoot());

    when(lookupPolicy.getClassLookupStrategy(anyString())).thenReturn(PARENT_FIRST);
  }

  @Test
  public void createsEmptyClassLoader() throws Exception {
    final ArtifactClassLoader artifactClassLoader =
        factory.create(SERVICE_ID, descriptor, getClass().getClassLoader(), lookupPolicy);
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();
    assertThat(classLoader.getURLs(), equalTo(new URL[0]));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validatesServiceFolder() throws Exception {
    File fakeServiceFolder = new File("./fake/folder/for/test");
    descriptor.setRootFolder(fakeServiceFolder);
    factory.create(SERVICE_ID, descriptor, getClass().getClassLoader(), lookupPolicy);
  }

  @Test
  public void addsClassesFolderToClassLoader() throws Exception {
    File classesFolder = serviceFolder.newFolder(ServiceClassLoaderFactory.CLASSES_DIR);

    final ArtifactClassLoader artifactClassLoader =
        factory.create(SERVICE_ID, descriptor, getClass().getClassLoader(), lookupPolicy);
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();
    assertThat(classLoader.getURLs(), equalTo(new URL[] {classesFolder.toURI().toURL()}));
  }

  @Test
  public void addJarsFromLibFolderToClassLoader() throws Exception {
    File libFolder = serviceFolder.newFolder(ServiceClassLoaderFactory.LIB_DIR);
    File jarFile = new File(libFolder, "dummy.jar");
    jarFile.createNewFile();

    final ArtifactClassLoader artifactClassLoader =
        factory.create(SERVICE_ID, descriptor, getClass().getClassLoader(), lookupPolicy);
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();
    assertThat(classLoader.getURLs(), equalTo(new URL[] {jarFile.toURI().toURL()}));
  }

  @Test
  public void ignoresNonJarsFilesFromLibFolder() throws Exception {
    File libFolder = serviceFolder.newFolder(ServiceClassLoaderFactory.LIB_DIR);
    File jarFile = new File(libFolder, "dummy.txt");
    jarFile.createNewFile();

    final ArtifactClassLoader artifactClassLoader =
        factory.create(SERVICE_ID, descriptor, getClass().getClassLoader(), lookupPolicy);
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();
    assertThat(classLoader.getURLs(), equalTo(new URL[] {}));
  }

  @Test
  public void usesClassLoaderLookupPolicy() throws Exception {
    final ArtifactClassLoader artifactClassLoader =
        factory.create(SERVICE_ID, descriptor, getClass().getClassLoader(), lookupPolicy);
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();

    final String className = "com.dummy.Foo";
    try {
      classLoader.loadClass(className);
      fail("Able to load an un-existent class");
    } catch (ClassNotFoundException e) {
      // Expected
    }

    verify(lookupPolicy).getClassLookupStrategy(className);
  }
}
