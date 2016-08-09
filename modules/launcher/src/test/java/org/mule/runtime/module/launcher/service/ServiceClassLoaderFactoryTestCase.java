/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;

import static junit.framework.TestCase.fail;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

  private final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
  @Rule
  public TemporaryFolder serviceFolder = new TemporaryFolder();
  private ServiceClassLoaderFactory factory = new ServiceClassLoaderFactory();
  private ServiceDescriptor descriptor;
  private ArtifactClassLoader parentClassLoader;

  @Before
  public void setUp() throws Exception {
    descriptor = new ServiceDescriptor();
    descriptor.setName("testService");
    descriptor.setRootFolder(serviceFolder.getRoot());

    parentClassLoader = mock(ArtifactClassLoader.class);
    when(parentClassLoader.getClassLoader()).thenReturn(getClass().getClassLoader());
    when(parentClassLoader.getClassLoaderLookupPolicy()).thenReturn(lookupPolicy);
  }

  @Test
  public void createsEmptyClassLoader() throws Exception {
    final ArtifactClassLoader artifactClassLoader = factory.create(parentClassLoader, descriptor);
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();
    assertThat(classLoader.getURLs(), equalTo(new URL[0]));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validatesServiceFolder() throws Exception {
    File fakeServiceFolder = new File("./fake/folder/for/test");
    descriptor.setRootFolder(fakeServiceFolder);
    factory.create(null, descriptor);
  }

  @Test
  public void addsClassesFolderToClassLoader() throws Exception {
    File classesFolder = serviceFolder.newFolder(ServiceClassLoaderFactory.CLASSES_DIR);

    final ArtifactClassLoader artifactClassLoader = factory.create(parentClassLoader, descriptor);
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();
    assertThat(classLoader.getURLs(), equalTo(new URL[] {classesFolder.toURI().toURL()}));
  }

  @Test
  public void addJarsFromLibFolderToClassLoader() throws Exception {
    File libFolder = serviceFolder.newFolder(ServiceClassLoaderFactory.LIB_DIR);
    File jarFile = new File(libFolder, "dummy.jar");
    jarFile.createNewFile();

    final ArtifactClassLoader artifactClassLoader = factory.create(parentClassLoader, descriptor);
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();
    assertThat(classLoader.getURLs(), equalTo(new URL[] {jarFile.toURI().toURL()}));
  }

  @Test
  public void ignoresNonJarsFilesFromLibFolder() throws Exception {
    File libFolder = serviceFolder.newFolder(ServiceClassLoaderFactory.LIB_DIR);
    File jarFile = new File(libFolder, "dummy.txt");
    jarFile.createNewFile();

    final ArtifactClassLoader artifactClassLoader = factory.create(parentClassLoader, descriptor);
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();
    assertThat(classLoader.getURLs(), equalTo(new URL[] {}));
  }

  @Test
  public void usesClassLoaderLookupPolicy() throws Exception {
    final ArtifactClassLoader artifactClassLoader = factory.create(parentClassLoader, descriptor);
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();

    final String className = "com.dummy.Foo";
    try {
      classLoader.loadClass(className);
      fail("Able to load an un-existent class");
    } catch (ClassNotFoundException e) {
      // Expected
    }

    verify(lookupPolicy).getLookupStrategy(className);
  }

}
