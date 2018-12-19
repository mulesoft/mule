/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.util.Optional.empty;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppDataFolder;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifactFactory;
import org.mule.runtime.module.deployment.internal.util.ObservableList;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FileUtils.class})
@PowerMockIgnore({"javax.management.*", "com.sun.org.apache.xerces.*"})
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

    mockStatic(FileUtils.class);
    PowerMockito.doThrow(new IOException()).when(FileUtils.class);
    deleteDirectory(Matchers.eq(getAppDataFolder(ARTIFACT_ID)));

    deployer.undeployArtifact(ARTIFACT_ID);
  }

  private Application createMockApplication() {
    Application artifact = mock(Application.class);
    ApplicationDescriptor descriptor = mock(ApplicationDescriptor.class);
    when(descriptor.getDataFolderName()).thenReturn(ARTIFACT_ID);
    when(artifact.getDescriptor()).thenReturn(descriptor);
    when(artifact.getArtifactName()).thenReturn(ARTIFACT_ID);
    return artifact;
  }
}
