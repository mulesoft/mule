/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.SINGLE_APP_MODE_PROPERTY;
import static org.mule.runtime.module.deployment.internal.singleapp.SingleAppApplicationDeployerBuilder.getSingleAppApplicationDeployerBuilder;
import static org.mule.runtime.module.deployment.internal.singleapp.SingleAppDomainDeployerBuilder.getSingleAppDomainDeployerBuilder;

import static java.lang.Boolean.getBoolean;
import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.internal.singleapp.SingleAppApplicationDeployerBuilder;
import org.mule.runtime.module.deployment.internal.singleapp.SingleAppDeploymentFileResolver;
import org.mule.runtime.module.deployment.internal.singleapp.SingleAppDeploymentService;
import org.mule.runtime.module.deployment.internal.singleapp.SingleAppDomainDeployerBuilder;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * A builder for {@link DeploymentService}.
 *
 * @since 4.7.0
 */
public class DeploymentServiceBuilder {

  public static final String DOMAIN_FACTORY_IS_NULL_ERROR_MESSAGE = "Domain Factory is null";
  public static final String APPLICATION_FACTORY_IS_NULL_ERROR_MESSAGE = "Application Factory is null";
  public static final String ARTIFACT_START_EXECUTOR_SUPPLIER_IS_NULL_ERROR_MESSAGE = "Artifact Start Executor Supplier is null";
  private DefaultDomainFactory domainFactory;
  private DefaultApplicationFactory applicationFactory;
  private Supplier<SchedulerService> artifactStartExecutorSupplier;

  private DeploymentServiceBuilder() {}

  public static DeploymentServiceBuilder deploymentServiceBuilder() {
    return new DeploymentServiceBuilder();
  }

  /**
   * @param domainFactory the {@link DefaultDomainFactory} to be used in the deployment service
   * @return the builder.
   */
  public DeploymentServiceBuilder withDomainFactory(DefaultDomainFactory domainFactory) {
    this.domainFactory = domainFactory;
    return this;
  }

  /**
   * @param applicationFactory the {@link DefaultApplicationFactory} to be used in the deployment service
   * @return the builder.
   */
  public DeploymentServiceBuilder withApplicationFactory(DefaultApplicationFactory applicationFactory) {
    this.applicationFactory = applicationFactory;
    return this;
  }

  /**
   * @param artifactStartExecutorSupplier the {@link Supplier<SchedulerService>} to be used in the deployment service
   * @return the builder.
   */
  public DeploymentServiceBuilder withArtifactStartExecutorSupplier(Supplier<SchedulerService> artifactStartExecutorSupplier) {
    this.artifactStartExecutorSupplier = artifactStartExecutorSupplier;
    return this;
  }

  public DeploymentService build() {
    requireNonNull(domainFactory, DOMAIN_FACTORY_IS_NULL_ERROR_MESSAGE);
    requireNonNull(applicationFactory, APPLICATION_FACTORY_IS_NULL_ERROR_MESSAGE);
    requireNonNull(artifactStartExecutorSupplier, ARTIFACT_START_EXECUTOR_SUPPLIER_IS_NULL_ERROR_MESSAGE);

    if (getBoolean(SINGLE_APP_MODE_PROPERTY)) {
      SingleAppDomainDeployerBuilder singleAppDomainDeployerBuilder = getSingleAppDomainDeployerBuilder();
      singleAppDomainDeployerBuilder
          .withDomainFactory(domainFactory)
          .withApplicationFactory(applicationFactory);

      SingleAppApplicationDeployerBuilder singleAppApplicationDeployerBuilder = getSingleAppApplicationDeployerBuilder();
      singleAppApplicationDeployerBuilder.withApplicationFactory(applicationFactory);

      return new SingleAppDeploymentService(singleAppDomainDeployerBuilder,
                                            singleAppApplicationDeployerBuilder,
                                            new SingleAppDeploymentFileResolver(),
                                            new ArrayList<>(),
                                            new ArrayList<>(),
                                            artifactStartExecutorSupplier);
    }

    return new MuleDeploymentService(domainFactory,
                                     applicationFactory,
                                     artifactStartExecutorSupplier);
  }
}
