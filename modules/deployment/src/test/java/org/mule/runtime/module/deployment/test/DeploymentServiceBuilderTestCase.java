/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test;



import static org.mule.runtime.api.util.MuleSystemProperties.SINGLE_APP_MODE_PROPERTY;
import static org.mule.runtime.module.deployment.internal.DeploymentServiceBuilder.deploymentServiceBuilder;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DeploymentServiceBuilderStory.DEPLOYMENT_SERVICE_BUILDER;

import static java.lang.Boolean.TRUE;
import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

import static org.junit.rules.ExpectedException.none;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.internal.MuleDeploymentService;
import org.mule.runtime.module.deployment.internal.singleapp.SingleAppDeploymentService;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


@Feature(APP_DEPLOYMENT)
@Story(DEPLOYMENT_SERVICE_BUILDER)
public class DeploymentServiceBuilderTestCase {

  public static final String TEST_TERMINATION_FILE_LOG_MESSAGE = "URI is not absolute";

  @Rule
  public ExpectedException expectedException = none();

  @After
  public void after() {
    clearProperty(SINGLE_APP_MODE_PROPERTY);
  }

  @Test
  public void domainFactoryNotNull() {
    expectedException.expectMessage("Domain Factory is null");

    Supplier<SchedulerService> artifactStartExecutorSupplier = mock(Supplier.class);
    DefaultApplicationFactory applicationFactory = mock(DefaultApplicationFactory.class);

    deploymentServiceBuilder().withDomainFactory(null)
        .withArtifactStartExecutorSupplier(artifactStartExecutorSupplier)
        .withApplicationFactory(applicationFactory).build();
  }

  @Test
  public void applicationFactoryNotNull() {
    expectedException.expectMessage("Application Factory is null");

    Supplier<SchedulerService> artifactStartExecutorSupplier = mock(Supplier.class);
    DefaultDomainFactory domainFactory = mock(DefaultDomainFactory.class);

    deploymentServiceBuilder().withDomainFactory(domainFactory)
        .withArtifactStartExecutorSupplier(artifactStartExecutorSupplier)
        .withApplicationFactory(null).build();
  }

  @Test
  public void artifactStartExecutorSupplierNotNull() {
    expectedException.expectMessage("Artifact Start Executor Supplier is null");

    DefaultDomainFactory domainFactory = mock(DefaultDomainFactory.class);
    DefaultApplicationFactory applicationFactory = mock(DefaultApplicationFactory.class);

    deploymentServiceBuilder().withDomainFactory(domainFactory)
        .withArtifactStartExecutorSupplier(null)
        .withApplicationFactory(applicationFactory).build();
  }

  @Test
  public void whenSystemPropertyIsSetSingleAppModeDeploymentServiceIsObtained() {
    setProperty(SINGLE_APP_MODE_PROPERTY, TRUE.toString());
    verifyDeploymentServiceImplementation(SingleAppDeploymentService.class);
  }

  @Test
  public void whenNoSystemPropertyIsSetSingleAppModeDeploymentServiceIsObtained() {
    verifyDeploymentServiceImplementation(MuleDeploymentService.class);
  }

  @Test
  public void singleAppDeploymentServiceIsConfiguredWithOnDeploymentErrorWriteToFileHandler() throws Exception {
    setProperty(SINGLE_APP_MODE_PROPERTY, TRUE.toString());
    DefaultDomainFactory domainFactory = mock(DefaultDomainFactory.class);
    DefaultApplicationFactory applicationFactory = mock(DefaultApplicationFactory.class);
    Supplier<SchedulerService> artifactStartExecutorSupplier = mock(Supplier.class);
    Consumer<Throwable> throwableConsumer = mock(Consumer.class);

    DeploymentService deploymentService = deploymentServiceBuilder().withDomainFactory(domainFactory)
        .withArtifactStartExecutorSupplier(artifactStartExecutorSupplier)
        .withApplicationFactory(applicationFactory)
        .withDeploymentFailureThrowableConsumer(throwableConsumer)
        .build();

    deploymentService.deploy(new URI("unknown-uri"));
    String terminationFileContent = new String(readAllBytes(get(getProperty("java.io.tmpdir") + getProperty("file.separator")
        + "termination_log_set_by_env_property")));
    assertThat(terminationFileContent, equalTo(TEST_TERMINATION_FILE_LOG_MESSAGE));
    verify(throwableConsumer).accept(any());
  }

  private static void verifyDeploymentServiceImplementation(Class expectedClass) {
    DefaultDomainFactory domainFactory = mock(DefaultDomainFactory.class);
    DefaultApplicationFactory applicationFactory = mock(DefaultApplicationFactory.class);
    Supplier<SchedulerService> artifactStartExecutorSupplier = mock(Supplier.class);

    DeploymentService deploymentService = deploymentServiceBuilder().withDomainFactory(domainFactory)
        .withArtifactStartExecutorSupplier(artifactStartExecutorSupplier)
        .withApplicationFactory(applicationFactory).build();

    assertThat(deploymentService, instanceOf(expectedClass));
  }
}
