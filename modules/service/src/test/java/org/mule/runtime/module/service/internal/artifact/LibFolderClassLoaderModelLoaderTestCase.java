/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.artifact;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.SERVICE;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.service.internal.artifact.LibFolderClassLoaderModelLoader.LIB_FOLDER;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LibFolderClassLoaderModelLoaderTestCase extends AbstractMuleTestCase {

  private final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);

  @Rule
  public TemporaryFolder serviceFolder = new TemporaryFolder();
  private LibFolderClassLoaderModelLoader classLoaderModelLoader = new LibFolderClassLoaderModelLoader();
  private ServiceDescriptor descriptor;

  @Before
  public void setUp() throws Exception {
    descriptor = new ServiceDescriptor("testService");
    descriptor.setRootFolder(serviceFolder.getRoot());

    when(lookupPolicy.getClassLookupStrategy(anyString())).thenReturn(PARENT_FIRST);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validatesServiceFolder() throws Exception {
    File fakeServiceFolder = new File("./fake/folder/for/test");
    descriptor.setRootFolder(fakeServiceFolder);
    classLoaderModelLoader.load(fakeServiceFolder, emptyMap(), SERVICE);
  }

  @Test
  public void addsArtifactFolderToClassLoader() throws Exception {
    ClassLoaderModel classLoaderModel = classLoaderModelLoader.load(serviceFolder.getRoot(), emptyMap(), SERVICE);

    assertThat(classLoaderModel.getUrls(), hasItemInArray(serviceFolder.getRoot().toURI().toURL()));
  }

  @Test
  public void addJarsFromLibFolderToClassLoader() throws Exception {
    File libFolder = serviceFolder.newFolder(LIB_FOLDER);
    File jarFile = new File(libFolder, "dummy.jar");
    jarFile.createNewFile();


    ClassLoaderModel classLoaderModel = classLoaderModelLoader.load(serviceFolder.getRoot(), emptyMap(), SERVICE);

    assertThat(classLoaderModel.getUrls(), hasItemInArray(jarFile.toURI().toURL()));
  }

  @Test
  public void ignoresNonJarsFilesFromLibFolder() throws Exception {
    File libFolder = serviceFolder.newFolder(LIB_FOLDER);
    File jarFile = new File(libFolder, "dummy.txt");
    jarFile.createNewFile();

    ClassLoaderModel classLoaderModel = classLoaderModelLoader.load(serviceFolder.getRoot(), emptyMap(), SERVICE);

    // Contains only the service root
    assertThat(classLoaderModel.getUrls(), hasItemInArray(serviceFolder.getRoot().toURL()));
  }
}
