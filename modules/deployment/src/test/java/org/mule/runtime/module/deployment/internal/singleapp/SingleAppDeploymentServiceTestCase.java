/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal.singleapp;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.deployment.internal.singleapp.SingleAppDeploymentService.createSingleAppDeploymentService;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.SingleAppDeploymentStory.SINGLE_APP_DEPLOYMENT;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.InOrder;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.runtime.module.deployment.internal.ArtifactDeployer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

import java.util.function.Supplier;

@Feature(APP_DEPLOYMENT)
@Story(SINGLE_APP_DEPLOYMENT)
public class SingleAppDeploymentServiceTestCase {

  public static final String TEST_APPLICATION_NAME = "test-application-name";

  @Test
  public void deploymentListenersAreCorrectlyNotifiedOnSuccess() {
    ArtifactDeployer<DeployableArtifact<ApplicationDescriptor>> artifactDeployer = mock(ArtifactDeployer.class);
    Supplier<Application> applicationSupplier = mock(Supplier.class);
    Application application = mock(Application.class);
    when(application.getArtifactName()).thenReturn(TEST_APPLICATION_NAME);
    when(applicationSupplier.get()).thenReturn(application);
    DeploymentListener deploymentListener = mock(DeploymentListener.class);
    StartupListener startUpListener = mock(StartupListener.class);

    SingleAppDeploymentService singleAppDeploymentService =
        createSingleAppDeploymentService(artifactDeployer, applicationSupplier);
    singleAppDeploymentService.addDeploymentListener(deploymentListener);
    singleAppDeploymentService.addStartupListener(startUpListener);

    singleAppDeploymentService.start();

    InOrder inOrder = inOrder(deploymentListener, artifactDeployer, startUpListener);

    inOrder.verify(deploymentListener).onDeploymentStart(TEST_APPLICATION_NAME);
    inOrder.verify(artifactDeployer).deploy(application);
    inOrder.verify(deploymentListener).onDeploymentSuccess(TEST_APPLICATION_NAME);
    inOrder.verify(startUpListener).onAfterStartup();
    verify(deploymentListener, never()).onDeploymentFailure(any(), any());
  }

  @Test
  public void deploymentListenersAreCorrectlyNotifiedOnFailure() {
    ArtifactDeployer<DeployableArtifact<ApplicationDescriptor>> artifactDeployer = mock(ArtifactDeployer.class);
    Supplier<Application> applicationSupplier = mock(Supplier.class);
    Application application = mock(Application.class);
    when(application.getArtifactName()).thenReturn(TEST_APPLICATION_NAME);
    when(applicationSupplier.get()).thenReturn(application);
    DeploymentListener deploymentListener = mock(DeploymentListener.class);
    StartupListener startUpListener = mock(StartupListener.class);
    doThrow(new DeploymentException(createStaticMessage("Test deployment exception"))).when(artifactDeployer).deploy(application);
    SingleAppDeploymentService singleAppDeploymentService =
        createSingleAppDeploymentService(artifactDeployer, applicationSupplier);
    singleAppDeploymentService.addDeploymentListener(deploymentListener);
    singleAppDeploymentService.addStartupListener(startUpListener);

    try {
      singleAppDeploymentService.start();
    } catch (DeploymentException e) {
      // Nothing to do.
    }

    InOrder inOrder = inOrder(deploymentListener, artifactDeployer, startUpListener);

    inOrder.verify(deploymentListener).onDeploymentStart(TEST_APPLICATION_NAME);
    inOrder.verify(artifactDeployer).deploy(application);
    inOrder.verify(deploymentListener).onDeploymentFailure(eq(TEST_APPLICATION_NAME), any(DeploymentException.class));
    verify(startUpListener, never()).onAfterStartup();
    verify(deploymentListener, never()).onDeploymentSuccess(any());
  }
}
