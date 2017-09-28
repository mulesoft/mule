/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.artifact;

import org.mule.runtime.config.internal.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;

/**
 * Spring implementation of {@link ArtifactConfigurationProcessor} that parses the XML configuration files and generates the
 * runtime object using the spring bean container.
 *
 * @since 4.0
 */
public class SpringArtifactConfigurationProcessor implements ArtifactConfigurationProcessor {

  @Override
  public ArtifactContext createArtifactContext(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {
    SpringXmlConfigurationBuilder springXmlConfigurationBuilder =
        new SpringXmlConfigurationBuilder(artifactContextConfiguration.getConfigResources(),
                                          artifactContextConfiguration.getArtifactDeclaration(),
                                          artifactContextConfiguration.getArtifactProperties(),
                                          artifactContextConfiguration.getArtifactType(),
                                          artifactContextConfiguration.isEnableLazyInitialization(),
                                          artifactContextConfiguration.isDisableXmlValidations());
    artifactContextConfiguration.getParentContext()
        .ifPresent(parentMuleContext -> springXmlConfigurationBuilder.setParentContext(parentMuleContext));
    artifactContextConfiguration.getServiceConfigurators().stream()
        .forEach(springXmlConfigurationBuilder::addServiceConfigurator);
    springXmlConfigurationBuilder.configure(artifactContextConfiguration.getMuleContext());
    return springXmlConfigurationBuilder.createArtifactContext();
  }
}
