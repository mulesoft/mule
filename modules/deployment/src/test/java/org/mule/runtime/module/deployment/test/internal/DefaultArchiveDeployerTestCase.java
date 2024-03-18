/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static java.util.Optional.empty;

import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifactFactory;
import org.mule.runtime.module.deployment.internal.ArtifactDeployer;
import org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer;
import org.mule.runtime.module.deployment.internal.util.ObservableList;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;

import io.qameta.allure.Issue;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class DefaultArchiveDeployerTestCase extends AbstractMuleTestCase {

  private static final String ARTIFACT_ID = "test";

  @Rule
  public final ExpectedException expectedException = none();

  @Test
  public void ignoresErrorsWhileRemovingArtifactDataFolder() throws Exception {
    AbstractDeployableArtifactFactory artifactFactory = mock(AbstractDeployableArtifactFactory.class);
    ArtifactDeployer artifactDeployer = mock(ArtifactDeployer.class);
    DefaultArchiveDeployer<ApplicationDescriptor, Application> deployer = createDeployer(artifactDeployer, artifactFactory);

    deployer.setDeploymentListener(mock(DeploymentListener.class));

    deployer.deployArtifact(createMockApplication(), empty());

    deployer.undeployArtifact(ARTIFACT_ID);
  }

  @Test
  @Issue("W-15159880")
  public void doNotThrowNPEOnError() {
    AbstractDeployableArtifactFactory artifactFactory = mock(AbstractDeployableArtifactFactory.class);
    ArtifactDeployer artifactDeployer = mock(ArtifactDeployer.class);
    DefaultArchiveDeployer<ApplicationDescriptor, Application> deployer = createDeployer(artifactDeployer, artifactFactory);

    DeploymentListener deploymentListener = mock(DeploymentListener.class);
    RuntimeException exceptionWithNoCause = new RuntimeException("Exception with no cause");
    doThrow(exceptionWithNoCause).when(deploymentListener).onDeploymentStart(anyString());
    deployer.setDeploymentListener(deploymentListener);

    expectedException.expect(DeploymentException.class);
    deployer.deployArtifact(createMockApplication(), empty());

    verify(deploymentListener).onDeploymentFailure(anyString(), same(exceptionWithNoCause));
  }

  @NotNull
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
    when(artifact.getResourceFiles()).thenReturn(new File[0]);
    return artifact;
  }
}
