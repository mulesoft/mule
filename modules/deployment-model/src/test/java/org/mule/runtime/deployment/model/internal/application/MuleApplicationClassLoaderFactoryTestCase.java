/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.application;

import static java.lang.System.setProperty;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppLibFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.internal.nativelib.NativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MuleApplicationClassLoaderFactoryTestCase extends AbstractMuleTestCase {

  private static final String DOMAIN_NAME = "test-domain";
  private static final String APP_NAME = "test-app";
  private static final String APP_ID = "test-app-id";

  @Rule
  public TemporaryFolder tempMuleHome = new TemporaryFolder();

  private final ArtifactClassLoader parentArtifactClassLoader = mock(ArtifactClassLoader.class);
  private final ClassLoaderLookupPolicy classLoaderLookupPolicy = mock(ClassLoaderLookupPolicy.class);
  private final NativeLibraryFinderFactory nativeLibraryFinderFactory = mock(NativeLibraryFinderFactory.class);
  private final MuleApplicationClassLoaderFactory classLoaderFactory =
      new MuleApplicationClassLoaderFactory(nativeLibraryFinderFactory);
  private String previousMuleHome;
  private ApplicationDescriptor descriptor;

  @Before
  public void createAppClassLoader() throws IOException {
    // Creates folder structure
    previousMuleHome = setProperty(MULE_HOME_DIRECTORY_PROPERTY, tempMuleHome.getRoot().getAbsolutePath());

    // Add jar file on application's lib folder
    File libDir = getAppLibFolder(APP_NAME);
    assertThat(libDir.mkdirs(), is(true));
    final File appLibrary = new File(libDir, "appLibrary.jar");
    stringToFile(appLibrary.getAbsolutePath(), "Some text");

    when(classLoaderLookupPolicy.getClassLookupStrategy(anyString())).thenReturn(PARENT_FIRST);
    when(parentArtifactClassLoader.getClassLoaderLookupPolicy()).thenReturn(classLoaderLookupPolicy);
    when(parentArtifactClassLoader.getClassLoader()).thenReturn(getClass().getClassLoader());

    when(classLoaderLookupPolicy.extend(anyMap())).thenReturn(classLoaderLookupPolicy);
    descriptor = new ApplicationDescriptor(APP_NAME);
    descriptor.setArtifactLocation(new File(tempMuleHome.newFolder(), APP_NAME));
  }

  @After
  public void cleanUp() {
    if (previousMuleHome != null) {
      setProperty(MULE_HOME_DIRECTORY_PROPERTY, previousMuleHome);
    }
    FileUtils.deleteTree(tempMuleHome.getRoot());
  }

  @Test
  public void createsClassLoader() throws Exception {

    final MuleApplicationClassLoader artifactClassLoader =
        (MuleApplicationClassLoader) classLoaderFactory.create(APP_ID, parentArtifactClassLoader, descriptor, emptyList());

    verify(nativeLibraryFinderFactory).create(APP_NAME, new URL[0]);
    assertThat(artifactClassLoader.getParent(), is(parentArtifactClassLoader.getClassLoader()));
    assertThat(artifactClassLoader.getArtifactId(), is(APP_ID));
  }
}
