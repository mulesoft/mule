/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static java.net.URI.create;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.deployment.internal.DisabledDeploymentService;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DisabledDeploymentServiceTestCase extends AbstractMuleTestCase {

  public static final URI ARCHIVE_URI = create("");
  public static final String APP = "app";
  private final DisabledDeploymentService disabledDeploymentService = new DisabledDeploymentService();

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void deployNotSupported() throws IOException {
    expectedException.expectMessage("Application deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.deploy(ARCHIVE_URI);
  }

  @Test
  public void deployWithPropertiesNotSupported() throws IOException {
    expectedException.expectMessage("Application deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.deploy(ARCHIVE_URI, mock(Properties.class));
  }

  @Test
  public void undeployNotSupported() {
    expectedException.expectMessage("Application undeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.undeploy(APP);
  }

  @Test
  public void redeployNotsupported() {
    expectedException.expectMessage("Application redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.redeploy(APP);
  }

  @Test
  public void redeployWithPropertiesNotSupported() {
    expectedException.expectMessage("Application redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.redeploy(APP, mock(Properties.class));
  }

  @Test
  public void redeployWithURIAndPropertiesNotSupported() throws IOException {
    expectedException.expectMessage("Application redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.redeploy(ARCHIVE_URI, mock(Properties.class));
  }

  @Test
  public void redeployWithURINotSupported() throws IOException {
    expectedException.expectMessage("Application redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.redeploy(ARCHIVE_URI);
  }

  @Test
  public void deployDomainNotSupported() {
    expectedException.expectMessage("Domain deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.deployDomain(ARCHIVE_URI);
  }

  @Test
  public void deployDomainBundleNotSupported() throws IOException {
    expectedException.expectMessage("Domain bundle deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.deployDomainBundle(ARCHIVE_URI);
  }

  @Test
  public void deployDomainWithPropertiesNotSupported() {
    expectedException.expectMessage("Domain deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.deployDomain(ARCHIVE_URI, mock(Properties.class));
  }

  @Test
  public void undeployDomainNotSupported() {
    expectedException.expectMessage("Domain undeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.undeployDomain(APP);
  }

  @Test
  public void redeployDomainNotSupported() {
    expectedException.expectMessage("Domain redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.redeployDomain(APP);
  }

  @Test
  public void redeployDomainWithPropertiesNotSupported() {
    expectedException.expectMessage("Domain redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    disabledDeploymentService.redeployDomain(APP, mock(Properties.class));
  }

  @Test
  public void deploymentListeners() {
    DisabledDeploymentService testDisabledDeploymentService = new DisabledDeploymentService();

    DeploymentListener deploymentListener1 = mock(DeploymentListener.class);
    DeploymentListener deploymentListener2 = mock(DeploymentListener.class);
    testDisabledDeploymentService.addDeploymentListener(deploymentListener1);
    testDisabledDeploymentService.addDeploymentListener(deploymentListener2);

    assertThat(testDisabledDeploymentService.getApplicationDeploymentListeners(),
               hasItems(deploymentListener1, deploymentListener2));

    testDisabledDeploymentService.removeDeploymentListener(deploymentListener1);

    assertThat(testDisabledDeploymentService.getApplicationDeploymentListeners(), not(hasItem(deploymentListener1)));
    assertThat(testDisabledDeploymentService.getApplicationDeploymentListeners(), hasItem(deploymentListener2));
  }

  @Test
  public void domainDeploymentListeners() {
    DisabledDeploymentService testDisabledDeploymentService = new DisabledDeploymentService();

    DeploymentListener deploymentListener1 = mock(DeploymentListener.class);
    DeploymentListener deploymentListener2 = mock(DeploymentListener.class);
    testDisabledDeploymentService.addDomainDeploymentListener(deploymentListener1);
    testDisabledDeploymentService.addDomainDeploymentListener(deploymentListener2);

    assertThat(testDisabledDeploymentService.getDomainDeploymentListeners(),
               hasItems(deploymentListener1, deploymentListener2));

    testDisabledDeploymentService.removeDomainDeploymentListener(deploymentListener1);

    assertThat(testDisabledDeploymentService.getDomainDeploymentListeners(), not(hasItem(deploymentListener1)));
    assertThat(testDisabledDeploymentService.getDomainDeploymentListeners(), hasItem(deploymentListener2));
  }

  @Test
  public void startupListeners() {
    DisabledDeploymentService testDisabledDeploymentService = new DisabledDeploymentService();

    StartupListener startUpListener1 = mock(StartupListener.class);
    StartupListener startUpListener2 = mock(StartupListener.class);
    testDisabledDeploymentService.addStartupListener(startUpListener1);
    testDisabledDeploymentService.addStartupListener(startUpListener2);

    assertThat(testDisabledDeploymentService.getStartupListeners(), hasItems(startUpListener1, startUpListener2));
  }

  @Test
  public void applications() {
    DisabledDeploymentService testDisabledDeploymentService = new DisabledDeploymentService();

    Application application1 = mock(Application.class);
    when(application1.getArtifactName()).thenReturn("applicationName1");

    Application application2 = mock(Application.class);
    when(application2.getArtifactName()).thenReturn("applicationName2");

    testDisabledDeploymentService.addApplication(application1);
    testDisabledDeploymentService.addApplication(application2);

    assertThat(testDisabledDeploymentService.getApplications(), hasSize(2));

    Application application = testDisabledDeploymentService.findApplication("applicationName1");
    assertThat(application, equalTo(application1));

    application = testDisabledDeploymentService.findApplication("applicationName2");
    assertThat(application, equalTo(application2));
  }

  @Test
  public void domains() {
    DisabledDeploymentService testDisabledDeploymentService = new DisabledDeploymentService();

    Domain domain1 = mock(Domain.class);
    when(domain1.getArtifactName()).thenReturn("domain1");

    Domain domain2 = mock(Domain.class);
    when(domain2.getArtifactName()).thenReturn("domain2");

    testDisabledDeploymentService.addDomain(domain1);
    testDisabledDeploymentService.addDomain(domain2);

    assertThat(testDisabledDeploymentService.getDomains(), hasSize(2));

    Domain domain = testDisabledDeploymentService.findDomain("domain1");
    assertThat(domain, equalTo(domain1));

    domain = testDisabledDeploymentService.findDomain("domain2");
    assertThat(domain, equalTo(domain2));
  }
}
