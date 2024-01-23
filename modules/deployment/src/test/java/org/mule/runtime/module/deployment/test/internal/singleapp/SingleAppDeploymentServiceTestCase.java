/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal.singleapp;

import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.SingleAppDeploymentStory.SINGLE_APP_DEPLOYMENT;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static java.net.URI.create;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jetbrains.annotations.NotNull;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer;
import org.mule.runtime.module.deployment.internal.DeploymentFileResolver;
import org.mule.runtime.module.deployment.internal.DomainArchiveDeployer;
import org.mule.runtime.module.deployment.internal.singleapp.SingleAppApplicationDeployerBuilder;
import org.mule.runtime.module.deployment.internal.singleapp.SingleAppDeploymentService;
import org.mule.runtime.module.deployment.internal.singleapp.SingleAppDomainDeployerBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(APP_DEPLOYMENT)
@Story(SINGLE_APP_DEPLOYMENT)
public class SingleAppDeploymentServiceTestCase extends AbstractMuleTestCase {

  public static final URI ARCHIVE_URI = create("");
  private static final String APP = "APP";
  public static final String DOMAIN = "DOMAIN";

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void exceptionOnSingleAppDeploymentModeWhenApplicationsAlreadyDeployed() throws IOException {
    expectedException.expectMessage("A deployment cannot be done if there is an already deployed app in single app mode.");
    expectedException.expect(UnsupportedOperationException.class);
    SingleAppDeploymentService singleAppDeploymentService = getSingleAppDeploymentService();
    singleAppDeploymentService.deploy(ARCHIVE_URI);
  }

  @NotNull
  private static SingleAppDeploymentService getSingleAppDeploymentService() {
    DomainArchiveDeployer appDomainDeployer = mock(DomainArchiveDeployer.class);
    SingleAppDomainDeployerBuilder singleAppDomainDeployerBuilder = mockSingleAppDomainDeployerBuilder(appDomainDeployer);
    SingleAppApplicationDeployerBuilder singleAppApplicationDeployerBuilder = mockSingleAppApplicationDeployerBuilder();
    DefaultArchiveDeployer archiveDeployer = mock(DefaultArchiveDeployer.class);
    when(singleAppApplicationDeployerBuilder.build()).thenReturn(archiveDeployer);
    List<Domain> domains = mock(List.class);
    List<Application> applications = mock(List.class);
    when(applications.isEmpty()).thenReturn(false);
    DeploymentFileResolver fileResolver = mock(DeploymentFileResolver.class);
    SchedulerService schedulerService = mock(SchedulerService.class);
    SingleAppDeploymentService singleAppDeploymentService =
        new SingleAppDeploymentService(singleAppDomainDeployerBuilder,
                                       singleAppApplicationDeployerBuilder,
                                       fileResolver,
                                       () -> schedulerService);
    return singleAppDeploymentService;
  }

  @Test
  public void whenSingleAppDeploymentModeDeployIfApplicationsAreNotDeployed() throws IOException {
    DomainArchiveDeployer appDomainDeployer = mock(DomainArchiveDeployer.class);
    SingleAppDomainDeployerBuilder singleAppDomainDeployerBuilder = mockSingleAppDomainDeployerBuilder(appDomainDeployer);
    SingleAppApplicationDeployerBuilder singleAppApplicationDeployerBuilder = mockSingleAppApplicationDeployerBuilder();
    DefaultArchiveDeployer archiveDeployer = mock(DefaultArchiveDeployer.class);
    when(singleAppApplicationDeployerBuilder.build()).thenReturn(archiveDeployer);
    List<Domain> domains = mock(List.class);
    List<Application> applications = mock(List.class);
    when(applications.isEmpty()).thenReturn(true);
    DeploymentFileResolver fileResolver = mock(DeploymentFileResolver.class);
    File file = mock(File.class);
    when(file.getName()).thenReturn("test.jar");
    when(fileResolver.resolve(any())).thenReturn(file);

    SchedulerService schedulerService = mock(SchedulerService.class);
    SingleAppDeploymentService singleAppDeploymentService =
        new SingleAppDeploymentService(singleAppDomainDeployerBuilder,
                                       singleAppApplicationDeployerBuilder,
                                       fileResolver,
                                       () -> schedulerService);

    singleAppDeploymentService.deploy(ARCHIVE_URI);
    verify(archiveDeployer).deployPackagedArtifact(eq(ARCHIVE_URI), any());
  }

  @Test
  public void whenSingleAppDeploymentModeDeployIfApplicationsDeploymentFailCallbackIsInvoked() throws IOException {
    DomainArchiveDeployer appDomainDeployer = mock(DomainArchiveDeployer.class);
    SingleAppDomainDeployerBuilder singleAppDomainDeployerBuilder = mockSingleAppDomainDeployerBuilder(appDomainDeployer);
    SingleAppApplicationDeployerBuilder singleAppApplicationDeployerBuilder = mockSingleAppApplicationDeployerBuilder();
    DefaultArchiveDeployer archiveDeployer = mock(DefaultArchiveDeployer.class);
    when(singleAppApplicationDeployerBuilder.build()).thenReturn(archiveDeployer);
    List<Domain> domains = mock(List.class);
    List<Application> applications = mock(List.class);
    when(applications.isEmpty()).thenReturn(true);
    DeploymentFileResolver fileResolver = mock(DeploymentFileResolver.class);
    File file = mock(File.class);
    when(file.getName()).thenReturn("test.jar");
    when(fileResolver.resolve(any())).thenReturn(file);

    SchedulerService schedulerService = mock(SchedulerService.class);

    SingleAppDeploymentService singleAppDeploymentService =
        new SingleAppDeploymentService(singleAppDomainDeployerBuilder,
                                       singleAppApplicationDeployerBuilder,
                                       fileResolver,
                                       () -> schedulerService);

    Consumer<Throwable> failConsumer = mock(Consumer.class);
    when(archiveDeployer.deployPackagedArtifact(eq(ARCHIVE_URI), any())).thenThrow(RuntimeException.class);
    singleAppDeploymentService.onDeploymentError(failConsumer);
    singleAppDeploymentService.deploy(ARCHIVE_URI);
    verify(archiveDeployer).deployPackagedArtifact(eq(ARCHIVE_URI), any());
    verify(failConsumer).accept(any(RuntimeException.class));
  }


  @Test
  public void undeployNotSupported() throws IOException {
    expectedException.expectMessage("Application undeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    getSingleAppDeploymentService().undeploy(APP);
  }

  @Test
  public void redeployNotsupported() {
    expectedException.expectMessage("Application redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    getSingleAppDeploymentService().redeploy(APP);
  }

  @Test
  public void redeployWithPropertiesNotSupported() {
    expectedException.expectMessage("Application redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    getSingleAppDeploymentService().redeploy(APP, mock(Properties.class));
  }

  @Test
  public void redeployWithURIAndPropertiesNotSupported() throws IOException {
    expectedException.expectMessage("Application redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    getSingleAppDeploymentService().redeploy(ARCHIVE_URI, mock(Properties.class));
  }

  @Test
  public void redeployWithURINotSupported() throws IOException {
    expectedException.expectMessage("Application redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    getSingleAppDeploymentService().redeploy(ARCHIVE_URI);
  }

  @Test
  public void deployDomainNotSupported() throws IOException {
    expectedException.expectMessage("Domain deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    getSingleAppDeploymentService().deployDomain(ARCHIVE_URI);
  }

  @Test
  public void deployDomainBundleNotSupported() throws IOException {
    expectedException.expectMessage("Domain bundle deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    getSingleAppDeploymentService().deployDomainBundle(ARCHIVE_URI);
  }

  @Test
  public void deployDomainWithPropertiesNotSupported() throws IOException {
    expectedException.expectMessage("Domain deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    getSingleAppDeploymentService().deployDomain(ARCHIVE_URI, mock(Properties.class));
  }

  @Test
  public void undeployDomainNotSupported() {
    expectedException.expectMessage("Domain undeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    getSingleAppDeploymentService().undeployDomain(APP);
  }

  @Test
  public void redeployDomainNotSupported() {
    expectedException.expectMessage("Domain redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    getSingleAppDeploymentService().redeployDomain(APP);
  }

  @Test
  public void redeployDomainWithPropertiesNotSupported() {
    expectedException.expectMessage("Domain redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    getSingleAppDeploymentService().redeployDomain(APP, mock(Properties.class));
  }

  @Test
  public void deploymentListeners() {
    SingleAppDeploymentService singleAppDeploymentService = getSingleAppDeploymentService();

    DeploymentListener deploymentListener1 = mock(DeploymentListener.class);
    DeploymentListener deploymentListener2 = mock(DeploymentListener.class);
    singleAppDeploymentService.addDeploymentListener(deploymentListener1);
    singleAppDeploymentService.addDeploymentListener(deploymentListener2);

    singleAppDeploymentService.getApplicationDeploymentListener().onDeploymentSuccess(APP);

    verify(deploymentListener1).onDeploymentSuccess(APP);
    verify(deploymentListener2).onDeploymentSuccess(APP);

    singleAppDeploymentService.removeDeploymentListener(deploymentListener1);

    singleAppDeploymentService.getApplicationDeploymentListener().onDeploymentSuccess(APP);

    verify(deploymentListener1, times(1)).onDeploymentSuccess(APP);
    verify(deploymentListener2, times(2)).onDeploymentSuccess(APP);
  }

  @Test
  public void domainDeploymentListeners() {
    SingleAppDeploymentService singleAppDeploymentService = getSingleAppDeploymentService();

    DeploymentListener deploymentListener1 = mock(DeploymentListener.class);
    DeploymentListener deploymentListener2 = mock(DeploymentListener.class);
    singleAppDeploymentService.addDomainDeploymentListener(deploymentListener1);
    singleAppDeploymentService.addDomainDeploymentListener(deploymentListener2);

    singleAppDeploymentService.getDomainDeploymentListener().onDeploymentSuccess(DOMAIN);

    verify(deploymentListener1).onDeploymentSuccess(DOMAIN);
    verify(deploymentListener2).onDeploymentSuccess(DOMAIN);

    singleAppDeploymentService.removeDomainDeploymentListener(deploymentListener1);

    singleAppDeploymentService.getDomainDeploymentListener().onDeploymentSuccess(DOMAIN);

    verify(deploymentListener1, times(1)).onDeploymentSuccess(DOMAIN);
    verify(deploymentListener2, times(2)).onDeploymentSuccess(DOMAIN);
  }

  @Test
  public void startupListeners() {
    SingleAppDeploymentService singleAppDeploymentService = getSingleAppDeploymentService();

    StartupListener startUpListener1 = mock(StartupListener.class);
    StartupListener startUpListener2 = mock(StartupListener.class);
    singleAppDeploymentService.addStartupListener(startUpListener1);
    singleAppDeploymentService.addStartupListener(startUpListener2);

    assertThat(singleAppDeploymentService.getStartupListeners(), hasItems(startUpListener1, startUpListener2));
  }

  private static SingleAppApplicationDeployerBuilder mockSingleAppApplicationDeployerBuilder() {
    SingleAppApplicationDeployerBuilder singleAppApplicationDeployerBuilder = mock(SingleAppApplicationDeployerBuilder.class);
    when(singleAppApplicationDeployerBuilder.withApplicationDeployer(any())).thenReturn(singleAppApplicationDeployerBuilder);
    when(singleAppApplicationDeployerBuilder.withApplicationDeploymentListener(any()))
        .thenReturn(singleAppApplicationDeployerBuilder);
    when(singleAppApplicationDeployerBuilder.withApplications(any())).thenReturn(singleAppApplicationDeployerBuilder);
    when(singleAppApplicationDeployerBuilder.withApplicationFactory(any())).thenReturn(singleAppApplicationDeployerBuilder);
    return singleAppApplicationDeployerBuilder;
  }

  private static SingleAppDomainDeployerBuilder mockSingleAppDomainDeployerBuilder(DomainArchiveDeployer appDomainDeployer) {
    SingleAppDomainDeployerBuilder singleAppDomainDeployerBuilder = mock(SingleAppDomainDeployerBuilder.class);
    when(singleAppDomainDeployerBuilder.withDomains(any())).thenReturn(singleAppDomainDeployerBuilder);
    when(singleAppDomainDeployerBuilder.withApplicationDeployer(any())).thenReturn(singleAppDomainDeployerBuilder);
    when(singleAppDomainDeployerBuilder.withDomainFactory(any())).thenReturn(singleAppDomainDeployerBuilder);
    when(singleAppDomainDeployerBuilder.withDomainArtifactDeployer(any())).thenReturn(singleAppDomainDeployerBuilder);
    when(singleAppDomainDeployerBuilder.withApplicationFactory(any())).thenReturn(singleAppDomainDeployerBuilder);
    when(singleAppDomainDeployerBuilder.withApplications(any())).thenReturn(singleAppDomainDeployerBuilder);
    when(singleAppDomainDeployerBuilder.withDeploymentService(any())).thenReturn(singleAppDomainDeployerBuilder);
    when(singleAppDomainDeployerBuilder.withApplicationArtifactDeployer(any())).thenReturn(singleAppDomainDeployerBuilder);
    when(singleAppDomainDeployerBuilder.withApplicationDeploymentListener(any())).thenReturn(singleAppDomainDeployerBuilder);
    when(singleAppDomainDeployerBuilder.withDomainDeploymentListener(any())).thenReturn(singleAppDomainDeployerBuilder);
    when(singleAppDomainDeployerBuilder.build()).thenReturn(appDomainDeployer);
    return singleAppDomainDeployerBuilder;
  }
}
