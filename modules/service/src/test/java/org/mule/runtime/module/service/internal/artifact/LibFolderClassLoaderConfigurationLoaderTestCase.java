/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.artifact;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.ArrayMatching.hasItemInArray;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.SERVICE;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.service.internal.artifact.LibFolderClassLoaderConfigurationLoader.LIB_FOLDER;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_CONFIGURATION_LOADER;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URL;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@Feature(CLASSLOADING_ISOLATION)
@Stories({@Story(CLASSLOADER_CONFIGURATION_LOADER), @Story(CLASSLOADER_CONFIGURATION)})
public class LibFolderClassLoaderConfigurationLoaderTestCase extends AbstractMuleTestCase {

  private final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);

  @Rule
  public TemporaryFolder serviceFolder = new TemporaryFolder();
  private LibFolderClassLoaderConfigurationLoader classLoaderConfigurationLoader = new LibFolderClassLoaderConfigurationLoader();
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
    classLoaderConfigurationLoader.load(fakeServiceFolder, emptyMap(), SERVICE);
  }

  @Test
  public void addsArtifactFolderToClassLoader() throws Exception {
    ClassLoaderConfiguration classLoaderModel = classLoaderConfigurationLoader.load(serviceFolder.getRoot(), emptyMap(), SERVICE);

    URL[] urls = classLoaderModel.getUrls();
    assertThat(urls, arrayWithSize(1));
    assertThat(urls, hasItemInArray(serviceFolder.getRoot().toURI().toURL()));
  }

  @Test
  public void addJarsFromLibFolderToClassLoader() throws Exception {
    File libFolder = serviceFolder.newFolder(LIB_FOLDER);
    File jarFile = new File(libFolder, "dummy.jar");
    jarFile.createNewFile();


    ClassLoaderConfiguration classLoaderModel = classLoaderConfigurationLoader.load(serviceFolder.getRoot(), emptyMap(), SERVICE);

    URL[] urls = classLoaderModel.getUrls();
    assertThat(urls, arrayWithSize(2));
    assertThat(urls, hasItemInArray(jarFile.toURI().toURL()));
  }

  @Test
  public void ignoresNonJarsFilesFromLibFolder() throws Exception {
    File libFolder = serviceFolder.newFolder(LIB_FOLDER);
    File jarFile = new File(libFolder, "dummy.txt");
    jarFile.createNewFile();

    ClassLoaderConfiguration classLoaderModel = classLoaderConfigurationLoader.load(serviceFolder.getRoot(), emptyMap(), SERVICE);

    // Contains only the service root
    assertThat(classLoaderModel.getUrls(), hasItemInArray(serviceFolder.getRoot().toURI().toURL()));
  }
}
