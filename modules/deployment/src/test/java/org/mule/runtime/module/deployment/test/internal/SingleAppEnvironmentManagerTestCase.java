/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.test.allure.AllureConstants.RuntimeEnvironment.RUNTIME_ENVIRONMENT;
import static org.mule.test.allure.AllureConstants.RuntimeEnvironment.RuntimeEnvironmentStory.SINGLE_APP_ENVIRONMENT;

import static java.net.URI.create;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.internal.SingleAppEnvironmentManager;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(RUNTIME_ENVIRONMENT)
@Story(SINGLE_APP_ENVIRONMENT)
public class SingleAppEnvironmentManagerTestCase extends AbstractMuleTestCase {

  public static final URI ARCHIVE_URI = create("");
  public static final String APP = "app";
  public static final String TEST_APP = "test_app";
  public static final String TEST_DOMAIN = "test_domain";
  private final SingleAppEnvironmentManager singleAppEnvironmentManager =
      new SingleAppEnvironmentManager(mock(DefaultDomainFactory.class),
                                      mock(DefaultApplicationFactory.class));

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void deployNotSupported() throws IOException {
    expectedException.expectMessage("Application deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.deploy(ARCHIVE_URI);
  }

  @Test
  public void deployWithPropertiesNotSupported() throws IOException {
    expectedException.expectMessage("Application deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.deploy(ARCHIVE_URI, mock(Properties.class));
  }

  @Test
  public void undeployNotSupported() {
    expectedException.expectMessage("Application undeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.undeploy(APP);
  }

  @Test
  public void redeployNotsupported() {
    expectedException.expectMessage("Application redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.redeploy(APP);
  }

  @Test
  public void redeployWithPropertiesNotSupported() {
    expectedException.expectMessage("Application redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.redeploy(APP, mock(Properties.class));
  }

  @Test
  public void redeployWithURIAndPropertiesNotSupported() throws IOException {
    expectedException.expectMessage("Application redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.redeploy(ARCHIVE_URI, mock(Properties.class));
  }

  @Test
  public void redeployWithURINotSupported() throws IOException {
    expectedException.expectMessage("Application redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.redeploy(ARCHIVE_URI);
  }

  @Test
  public void deployDomainNotSupported() {
    expectedException.expectMessage("Domain deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.deployDomain(ARCHIVE_URI);
  }

  @Test
  public void deployDomainBundleNotSupported() {
    expectedException.expectMessage("Domain bundle deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.deployDomainBundle(ARCHIVE_URI);
  }

  @Test
  public void deployDomainWithPropertiesNotSupported() {
    expectedException.expectMessage("Domain deploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.deployDomain(ARCHIVE_URI, mock(Properties.class));
  }

  @Test
  public void undeployDomainNotSupported() {
    expectedException.expectMessage("Domain undeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.undeployDomain(APP);
  }

  @Test
  public void redeployDomainNotSupported() {
    expectedException.expectMessage("Domain redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.redeployDomain(APP);
  }

  @Test
  public void redeployDomainWithPropertiesNotSupported() {
    expectedException.expectMessage("Domain redeploy operation not supported");
    expectedException.expect(UnsupportedOperationException.class);
    singleAppEnvironmentManager.redeployDomain(APP, mock(Properties.class));
  }

  @Test
  public void deploymentListeners() {
    SingleAppEnvironmentManager testSingleAppEnvironmentManager =
        new SingleAppEnvironmentManager(mock(DefaultDomainFactory.class),
                                        mock(DefaultApplicationFactory.class));

    DeploymentListener deploymentListener1 = mock(DeploymentListener.class);
    DeploymentListener deploymentListener2 = mock(DeploymentListener.class);
    testSingleAppEnvironmentManager.addDeploymentListener(deploymentListener1);
    testSingleAppEnvironmentManager.addDeploymentListener(deploymentListener2);

    testSingleAppEnvironmentManager.getApplicationDeploymentListener().onDeploymentSuccess(TEST_APP);

    verify(deploymentListener1).onDeploymentSuccess(TEST_APP);
    verify(deploymentListener2).onDeploymentSuccess(TEST_APP);

    testSingleAppEnvironmentManager.removeDeploymentListener(deploymentListener1);

    testSingleAppEnvironmentManager.getApplicationDeploymentListener().onDeploymentSuccess(TEST_APP);

    verify(deploymentListener1, times(1)).onDeploymentSuccess(TEST_APP);
    verify(deploymentListener2, times(2)).onDeploymentSuccess(TEST_APP);
  }

  @Test
  public void domainDeploymentListeners() {
    SingleAppEnvironmentManager testSingleAppEnvironmentManager =
        new SingleAppEnvironmentManager(mock(DefaultDomainFactory.class),
                                        mock(DefaultApplicationFactory.class));

    DeploymentListener deploymentListener1 = mock(DeploymentListener.class);
    DeploymentListener deploymentListener2 = mock(DeploymentListener.class);
    testSingleAppEnvironmentManager.addDomainDeploymentListener(deploymentListener1);
    testSingleAppEnvironmentManager.addDomainDeploymentListener(deploymentListener2);

    testSingleAppEnvironmentManager.getDomainDeploymentListener().onDeploymentSuccess(TEST_DOMAIN);

    verify(deploymentListener1).onDeploymentSuccess(TEST_DOMAIN);
    verify(deploymentListener2).onDeploymentSuccess(TEST_DOMAIN);

    testSingleAppEnvironmentManager.removeDomainDeploymentListener(deploymentListener1);

    testSingleAppEnvironmentManager.getDomainDeploymentListener().onDeploymentSuccess(TEST_DOMAIN);

    verify(deploymentListener1, times(1)).onDeploymentSuccess(TEST_DOMAIN);
    verify(deploymentListener2, times(2)).onDeploymentSuccess(TEST_DOMAIN);
  }

  @Test
  public void startupListeners() {
    SingleAppEnvironmentManager testSingleAppEnvironmentManager =
        new SingleAppEnvironmentManager(mock(DefaultDomainFactory.class),
                                        mock(DefaultApplicationFactory.class));

    StartupListener startUpListener1 = mock(StartupListener.class);
    StartupListener startUpListener2 = mock(StartupListener.class);
    testSingleAppEnvironmentManager.addStartupListener(startUpListener1);
    testSingleAppEnvironmentManager.addStartupListener(startUpListener2);

    assertThat(testSingleAppEnvironmentManager.getStartupListeners(), hasItems(startUpListener1, startUpListener2));
  }

  @Test
  public void applications() {
    SingleAppEnvironmentManager testSingleAppEnvironmentManager =
        new SingleAppEnvironmentManager(mock(DefaultDomainFactory.class),
                                        mock(DefaultApplicationFactory.class));

    Application application1 = mock(Application.class);
    when(application1.getArtifactName()).thenReturn("applicationName1");

    Application application2 = mock(Application.class);
    when(application2.getArtifactName()).thenReturn("applicationName2");

    testSingleAppEnvironmentManager.addApplication(application1);
    testSingleAppEnvironmentManager.addApplication(application2);

    assertThat(testSingleAppEnvironmentManager.getApplications(), hasSize(2));

    Application application = testSingleAppEnvironmentManager.findApplication("applicationName1");
    assertThat(application, equalTo(application1));

    application = testSingleAppEnvironmentManager.findApplication("applicationName2");
    assertThat(application, equalTo(application2));
  }

  @Test
  public void domains() {
    SingleAppEnvironmentManager testSingleAppEnvironmentManager =
        new SingleAppEnvironmentManager(mock(DefaultDomainFactory.class),
                                        mock(DefaultApplicationFactory.class));

    Domain domain1 = mock(Domain.class);
    when(domain1.getArtifactName()).thenReturn("domain1");

    Domain domain2 = mock(Domain.class);
    when(domain2.getArtifactName()).thenReturn("domain2");

    testSingleAppEnvironmentManager.addDomain(domain1);
    testSingleAppEnvironmentManager.addDomain(domain2);

    assertThat(testSingleAppEnvironmentManager.getDomains(), hasSize(2));

    Domain domain = testSingleAppEnvironmentManager.findDomain("domain1");
    assertThat(domain, equalTo(domain1));

    domain = testSingleAppEnvironmentManager.findDomain("domain2");
    assertThat(domain, equalTo(domain2));
  }

  @Test
  public void whenNoDomainIsSetAnExceptionIsRaised() {
    SingleAppEnvironmentManager testSingleAppEnvironmentManager =
        new SingleAppEnvironmentManager(mock(DefaultDomainFactory.class),
                                        mock(DefaultApplicationFactory.class));
    expectedException.expect(SingleAppEnvironmentManager.SingleAppEnvironmentException.class);
    expectedException.expectMessage("No mule domain path for single app");

    testSingleAppEnvironmentManager.startApp();
  }

  @Test
  public void whenNoAppIsSetAnExceptionIsRaised() {
    SingleAppEnvironmentManager testSingleAppEnvironmentManager =
        new SingleAppEnvironmentManager(mock(DefaultDomainFactory.class),
                                        mock(DefaultApplicationFactory.class));
    expectedException.expect(SingleAppEnvironmentManager.SingleAppEnvironmentException.class);
    expectedException.expectMessage("No mule domain path for single app");

    testSingleAppEnvironmentManager.startApp();
  }
}
