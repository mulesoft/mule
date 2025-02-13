/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.processor;

import static org.mule.runtime.config.api.ArtifactContextFactory.createArtifactContextFactory;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.api.ArtifactContextFactory;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.NullDomainMuleContextLifecycleStrategy;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;
import org.mule.runtime.deployment.model.internal.artifact.ImmutableArtifactContext;

/**
 * Base class providing a reusable template method that contains an {@link ArtifactAst} and delegates to
 * {@link ArtifactAstConfigurationBuilder} to create registry and populate the {@link MuleContext}.
 * 
 * @since 4.5
 */
abstract class AbstractAstConfigurationProcessor implements ArtifactConfigurationProcessor {

  @Override
  public ArtifactContext createArtifactContext(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {
    if (isEmpty(artifactContextConfiguration.getConfigResources())) {
      return contextForEmptyArtifact(artifactContextConfiguration);
    }

    ArtifactAst artifactAst = obtainArtifactAst(artifactContextConfiguration);

    ArtifactContextFactory configurationBuilder = createArtifactContextFactory(artifactAst,
                                                                               artifactContextConfiguration
                                                                                   .getArtifactProperties(),
                                                                               artifactContextConfiguration
                                                                                   .getArtifactType(),
                                                                               artifactContextConfiguration
                                                                                   .isEnableLazyInitialization(),
                                                                               artifactContextConfiguration
                                                                                   .isAddToolingObjectsToRegistry(),
                                                                               artifactContextConfiguration
                                                                                   .getServiceConfigurators(),
                                                                               artifactContextConfiguration
                                                                                   .getParentArtifactContext());

    configurationBuilder.configure(artifactContextConfiguration.getMuleContext());

    artifactAst.postConfigurationEnrichment();

    // This has to be done after enriching models
    return configurationBuilder.createArtifactContext();
  }

  protected ArtifactContext contextForEmptyArtifact(ArtifactContextConfiguration artifactContextConfiguration) {
    ((DefaultMuleContext) artifactContextConfiguration.getMuleContext())
        .setLifecycleStrategy(new NullDomainMuleContextLifecycleStrategy());
    return new ImmutableArtifactContext(artifactContextConfiguration.getMuleContext());
  }

  protected abstract ArtifactAst obtainArtifactAst(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException;
}
