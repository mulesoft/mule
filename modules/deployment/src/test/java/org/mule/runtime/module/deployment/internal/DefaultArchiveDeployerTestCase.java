/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.container.api.MuleFoldersUtil.getAppNativeLibrariesTempFolder;

import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifactFactory;
import org.mule.runtime.module.deployment.internal.util.ObservableList;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;

import io.qameta.allure.Issue;
import org.junit.Test;

@SmallTest
public class DefaultArchiveDeployerTestCase extends AbstractMuleTestCase {

  private static final String ARTIFACT_ID = "test";

  @Test
  public void ignoresErrorsWhileRemovingArtifactDataFolder() throws Exception {
    AbstractDeployableArtifactFactory artifactFactory = mock(AbstractDeployableArtifactFactory.class);

    ArtifactDeployer artifactDeployer = mock(ArtifactDeployer.class);

    DefaultArchiveDeployer<Application> deployer =
        new DefaultArchiveDeployer(artifactDeployer, artifactFactory, new ObservableList(), null, null);
    deployer.setDeploymentListener(mock(DeploymentListener.class));

    deployer.deployArtifact(createMockApplication(), empty());

    deployer.undeployArtifact(ARTIFACT_ID);
  }

  @Test
  @Issue("W-15894519")
  public void undeployDeletesTheNativeLibrariesTempFolder() {
    AbstractDeployableArtifactFactory artifactFactory = mock(AbstractDeployableArtifactFactory.class);
    ArtifactDeployer artifactDeployer = mock(ArtifactDeployer.class);
    DefaultArchiveDeployer<Application> deployer = createDeployer(artifactDeployer, artifactFactory);
    Application application = createMockApplicationWithoutExceptions();
    String loadedNativeLibrariesFolderName = application.getDescriptor().getLoadedNativeLibrariesFolderName();
    File nativeLibrariesTempFolder = getAppNativeLibrariesTempFolder(ARTIFACT_ID, loadedNativeLibrariesFolderName);

    nativeLibrariesTempFolder.mkdirs();

    deployer.setDeploymentListener(mock(DeploymentListener.class));
    deployer.deployArtifact(application, empty());
    assertTrue(nativeLibrariesTempFolder.exists());

    deployer.undeployArtifact(ARTIFACT_ID);
    assertFalse(nativeLibrariesTempFolder.exists());
  }

  private static DefaultArchiveDeployer createDeployer(ArtifactDeployer artifactDeployer,
                                                       AbstractDeployableArtifactFactory artifactFactory) {
    return new DefaultArchiveDeployer(artifactDeployer, artifactFactory, new ObservableList(), null, null);
  }

  private Application createMockApplication() {
    Application artifact = mock(Application.class);
    ApplicationDescriptor descriptor = mock(ApplicationDescriptor.class);
    when(descriptor.getDataFolderName()).thenReturn(ARTIFACT_ID).thenThrow(new RuntimeException((new IOException())));
    when(artifact.getDescriptor()).thenReturn(descriptor);
    when(artifact.getArtifactName()).thenReturn(ARTIFACT_ID);
    return artifact;
  }

  private Application createMockApplicationWithoutExceptions() {
    Application artifact = mock(Application.class);
    ApplicationDescriptor descriptor = mock(ApplicationDescriptor.class);
    when(descriptor.getDataFolderName()).thenReturn(ARTIFACT_ID);
    when(descriptor.getLoadedNativeLibrariesFolderName()).thenReturn(randomUUID().toString());
    when(artifact.getDescriptor()).thenReturn(descriptor);
    when(artifact.getArtifactName()).thenReturn(ARTIFACT_ID);
    when(artifact.getResourceFiles()).thenReturn(new File[0]);
    return artifact;
  }
}
