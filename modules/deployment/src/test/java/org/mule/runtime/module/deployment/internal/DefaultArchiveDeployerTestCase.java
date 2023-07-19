/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal;

import static java.util.Optional.empty;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifactFactory;
import org.mule.runtime.module.deployment.internal.util.ObservableList;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;

import org.junit.Test;

@SmallTest
public class DefaultArchiveDeployerTestCase extends AbstractMuleTestCase {

  private static final String ARTIFACT_ID = "test";

  @Test
  public void ignoresErrorsWhileRemovingArtifactDataFolder() throws Exception {
    AbstractDeployableArtifactFactory artifactFactory = mock(AbstractDeployableArtifactFactory.class);

    ArtifactDeployer artifactDeployer = mock(ArtifactDeployer.class);

    DefaultArchiveDeployer<ApplicationDescriptor, Application> deployer =
        new DefaultArchiveDeployer(artifactDeployer, artifactFactory, new ObservableList(), null, null);
    deployer.setDeploymentListener(mock(DeploymentListener.class));

    deployer.deployArtifact(createMockApplication(), empty());

    deployer.undeployArtifact(ARTIFACT_ID);
  }

  private Application createMockApplication() {
    Application artifact = mock(Application.class);
    ApplicationDescriptor descriptor = mock(ApplicationDescriptor.class);
    when(descriptor.getDataFolderName()).thenReturn(ARTIFACT_ID).thenThrow(new RuntimeException((new IOException())));
    when(artifact.getDescriptor()).thenReturn(descriptor);
    when(artifact.getArtifactName()).thenReturn(ARTIFACT_ID);
    return artifact;
  }
}
