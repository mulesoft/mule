/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DeploymentFailureStory.DEPLOYMENT_FAILURE;

import org.mule.runtime.core.api.util.concurrent.NamedThreadFactory;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.tck.SimpleUnitTestSupportScheduler;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(APP_DEPLOYMENT)
public class DefaultArtifactDeployerTestCase extends AbstractMuleWithTestLoggingSupportTestCase {

  @Rule
  public ExpectedException expected = none();

  private SimpleUnitTestSupportScheduler scheduler;

  @Before
  public void before() throws IOException {
    scheduler = new SimpleUnitTestSupportScheduler(1,
                                                   new NamedThreadFactory(DefaultArtifactDeployerTestCase.class.getSimpleName(),
                                                                          DefaultArtifactDeployerTestCase.class.getClassLoader()),
                                                   new AbortPolicy());
  }

  @After
  public void after() {
    scheduler.stop();
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
  public void deploymentStartFails() {
    final DefaultArtifactDeployer<DeployableArtifact> deployer = new DefaultArtifactDeployer<>(() -> scheduler);
    final DeployableArtifact artifact = mock(DeployableArtifact.class);
    when(artifact.getDescriptor()).thenReturn(new ApplicationDescriptor("testApp"));
    when(artifact.getArtifactName()).thenReturn("testApp");

    final Exception expectedException = new RuntimeException();
    doThrow(expectedException).when(artifact).start();

    expected.expect(DeploymentException.class);
    expected.expectCause(sameInstance(expectedException));
    deployer.deploy(artifact, true);
  }

}
