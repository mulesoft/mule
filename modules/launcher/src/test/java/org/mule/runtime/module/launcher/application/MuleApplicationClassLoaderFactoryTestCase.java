/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import static java.lang.System.setProperty;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.util.FileUtils.stringToFile;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getAppClassesFolder;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getAppLibFolder;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getMulePerAppLibFolder;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.launcher.MuleApplicationClassLoader;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.nativelib.NativeLibraryFinderFactory;
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

  @Rule
  public TemporaryFolder tempMuleHome = new TemporaryFolder();

  private String previousMuleHome;
  private final ArtifactClassLoader parentArtifactClassLoader = mock(ArtifactClassLoader.class);
  private final ClassLoaderLookupPolicy classLoaderLookupPolicy = mock(ClassLoaderLookupPolicy.class);
  private URL classesFolderUrl;
  private URL appLibraryUrl;
  private URL perAppLibraryUrl;

  @Before
  public void createAppClassLoader() throws IOException {
    // Creates folder structure
    previousMuleHome = setProperty(MULE_HOME_DIRECTORY_PROPERTY, tempMuleHome.getRoot().getAbsolutePath());

    // Add jar file on application's lib folder
    File libDir = getAppLibFolder(APP_NAME);
    assertThat(libDir.mkdirs(), is(true));
    final File appLibrary = new File(libDir, "appLibrary.jar");
    stringToFile(appLibrary.getAbsolutePath(), "Some text");

    // Add jar file on container's per-app folder
    File perAppLibFolder = getMulePerAppLibFolder();
    assertThat(perAppLibFolder.mkdirs(), is(true));
    final File perAppLibrary = new File(perAppLibFolder, "perAppLibrary.jar");
    stringToFile(perAppLibrary.getAbsolutePath(), "Some text");

    when(parentArtifactClassLoader.getClassLoaderLookupPolicy()).thenReturn(classLoaderLookupPolicy);
    when(parentArtifactClassLoader.getClassLoader()).thenReturn(getClass().getClassLoader());

    classesFolderUrl = getAppClassesFolder(APP_NAME).toURI().toURL();
    appLibraryUrl = appLibrary.toURI().toURL();
    perAppLibraryUrl = perAppLibrary.toURI().toURL();
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
    final NativeLibraryFinderFactory nativeLibraryFinderFactory = mock(NativeLibraryFinderFactory.class);

    MuleApplicationClassLoaderFactory classLoaderFactory = new MuleApplicationClassLoaderFactory(nativeLibraryFinderFactory);

    final ApplicationDescriptor descriptor = new ApplicationDescriptor();
    descriptor.setName(APP_NAME);
    descriptor.setDomain(DOMAIN_NAME);

    final MuleApplicationClassLoader artifactClassLoader =
        (MuleApplicationClassLoader) classLoaderFactory.create(parentArtifactClassLoader, descriptor, emptyList());

    verify(nativeLibraryFinderFactory).create(APP_NAME);
    assertThat(artifactClassLoader.getParent(), is(parentArtifactClassLoader.getClassLoader()));
    assertThat(artifactClassLoader.getURLs(), arrayContaining(classesFolderUrl, appLibraryUrl, perAppLibraryUrl));
  }
}
