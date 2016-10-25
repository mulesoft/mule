/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.artifact;

import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.MuleContextServiceConfigurator;
import org.mule.runtime.dsl.api.config.ArtifactConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring implementation of {@link ArtifactConfigurationProcessor} that parses the XML configuration files and generates the runtime
 * object using the spring bean container.
 *
 * @since 4.0
 */
public class SpringArtifactContextBuilder implements ArtifactConfigurationProcessor {

  @Override
  public ArtifactContext createArtifactContext(MuleContext muleContext, String[] configResources,
                                               ArtifactConfiguration artifactConfiguration,
                                               Map<String, String> artifactProperties,
                                               ArtifactType artifactType, boolean enableLazyInitialization,
                                               List<MuleContextServiceConfigurator> serviceConfigurators,
                                               Optional<MuleContext> parentContext)
      throws ConfigurationException {
    SpringXmlConfigurationBuilder springXmlConfigurationBuilder =
        new SpringXmlConfigurationBuilder(configResources, artifactConfiguration, artifactProperties, artifactType,
                                          enableLazyInitialization);
    parentContext.ifPresent(parentMuleContext -> springXmlConfigurationBuilder.setParentContext(parentMuleContext));
    springXmlConfigurationBuilder.configure(muleContext);
    serviceConfigurators.stream().forEach(springXmlConfigurationBuilder::addServiceConfigurator);
    return new SpringArtifactContext(springXmlConfigurationBuilder.getMuleArtifactContext());
  }

}
