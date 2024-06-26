/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.runtime.container.api.MuleFoldersUtil.getAppNativeLibrariesTempFolder;

import static java.lang.String.valueOf;
import static java.lang.System.mapLibraryName;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.activation.internal.classloader.MuleSharedDomainClassLoader;
import org.mule.runtime.module.artifact.activation.internal.nativelib.ArtifactCopyNativeLibraryFinder;
import org.mule.runtime.module.artifact.activation.internal.nativelib.NativeLibraryFinder;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifactFactory;
import org.mule.runtime.module.deployment.internal.ArtifactDeployer;
import org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class NativeLibrariesFileDeletionTestCase {

  private static final String ARTIFACT_ID = "application-test";
  private static final String NATIVE_LIBRARY = "native-library";
  private static final String DEFAULT_DOMAIN_NAME = "default";

  private final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);

  @Rule
  public TemporaryFolder nativeLibraryFolder = new TemporaryFolder();

  @Test
  @Issue("W-15894519")
  public void undeployDeletesTheNativeLibrariesTempFolder() {
    AbstractDeployableArtifactFactory artifactFactory = mock(AbstractDeployableArtifactFactory.class);
    ArtifactDeployer artifactDeployer = mock(ArtifactDeployer.class);
    DefaultArchiveDeployer<ApplicationDescriptor, Application> deployer = createDeployer(artifactDeployer, artifactFactory);
    Application application = createMockApplication();
    String loadedNativeLibrariesFolderName = application.getDescriptor().getLoadedNativeLibrariesFolderName();
    File nativeLibrariesTempFolder = getAppNativeLibrariesTempFolder(ARTIFACT_ID, loadedNativeLibrariesFolderName);

    nativeLibrariesTempFolder.mkdirs();

    deployer.setDeploymentListener(mock(DeploymentListener.class));
    deployer.deployArtifact(application, empty());
    assertTrue(nativeLibrariesTempFolder.exists());

    deployer.undeployArtifact(ARTIFACT_ID);
    assertFalse(nativeLibrariesTempFolder.exists());
  }

  @Test
  @Issue("W-15894519")
  public void undeployDeletesTheNativeLibrariesTempFolderWithACopiedNativeLibrary() throws Exception {
    AbstractDeployableArtifactFactory artifactFactory = mock(AbstractDeployableArtifactFactory.class);
    ArtifactDeployer artifactDeployer = mock(ArtifactDeployer.class);
    DefaultArchiveDeployer<ApplicationDescriptor, Application> deployer = createDeployer(artifactDeployer, artifactFactory);

    Application application = createMockApplication();
    String loadedNativeLibrariesFolderName = application.getDescriptor().getLoadedNativeLibrariesFolderName();
    File nativeLibrariesTempFolder = getAppNativeLibrariesTempFolder(ARTIFACT_ID, loadedNativeLibrariesFolderName);
    File copiedNativeLibrary = getCopiedNativeLibrary(nativeLibrariesTempFolder, createNativeLibraryFile());

    try (FileInputStream fileInputStream = new FileInputStream(copiedNativeLibrary)) {
      fileInputStream.read();

      deployer.setDeploymentListener(mock(DeploymentListener.class));
      deployer.deployArtifact(application, empty());
      assertTrue(copiedNativeLibrary.exists());
      assertTrue(nativeLibrariesTempFolder.exists());

      deployer.undeployArtifact(ARTIFACT_ID);
      assertFalse(copiedNativeLibrary.exists());
      assertFalse(nativeLibrariesTempFolder.exists());
    } catch (Exception e) {
      //
    }
  }

  private static DefaultArchiveDeployer createDeployer(ArtifactDeployer artifactDeployer,
                                                       AbstractDeployableArtifactFactory artifactFactory) {
    return new DefaultArchiveDeployer(artifactDeployer, artifactFactory, new ObservableList(), null, null);
  }

  private Application createMockApplication() {
    Application artifact = mock(Application.class);
    ApplicationDescriptor descriptor = mock(ApplicationDescriptor.class);
    when(descriptor.getDataFolderName()).thenReturn(ARTIFACT_ID);
    when(descriptor.getLoadedNativeLibrariesFolderName()).thenReturn(valueOf(randomUUID()));
    when(artifact.getDescriptor()).thenReturn(descriptor);
    when(artifact.getArtifactName()).thenReturn(ARTIFACT_ID);
    when(artifact.getResourceFiles()).thenReturn(new File[0]);
    return artifact;
  }

  private File createNativeLibraryFile() throws Exception {
    return createNativeLibraryFile(nativeLibraryFolder.getRoot(), mapLibraryName(NATIVE_LIBRARY));
  }

  private File createNativeLibraryFile(File folder, String libFileName) throws Exception {
    File libraryFile = new File(folder, libFileName);
    assertTrue(FileUtils.createFile(libraryFile.getAbsolutePath()).exists());
    return libraryFile;
  }

  private File getCopiedNativeLibrary(File nativeLibrariesTempFolder, File nativeLibrary) throws MalformedURLException {
    NativeLibraryFinder nativeLibraryFinder =
        new ArtifactCopyNativeLibraryFinder(nativeLibrariesTempFolder, new URL[] {nativeLibrary.toURL()});
    MuleSharedDomainClassLoader classLoader = new MuleSharedDomainClassLoader(new ArtifactDescriptor(DEFAULT_DOMAIN_NAME),
                                                                              getClass().getClassLoader(), lookupPolicy,
                                                                              emptyList(), nativeLibraryFinder);
    return new File(classLoader.findLibrary(NATIVE_LIBRARY));
  }
}
