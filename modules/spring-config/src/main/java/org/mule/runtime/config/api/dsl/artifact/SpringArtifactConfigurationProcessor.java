/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.artifact;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

import org.mule.runtime.config.internal.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.NullDomainMuleContextLifecycleStrategy;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;
import org.mule.runtime.deployment.model.internal.artifact.ImmutableArtifactContext;

/**
 * Spring implementation of {@link ArtifactConfigurationProcessor} that parses the XML configuration files and generates the
 * runtime object using the spring bean container.
 *
 * @since 4.0
 * 
 * @deprecated Use {@link AstArtifactConfigurationProcessor} instead.
 */
@Deprecated
public final class SpringArtifactConfigurationProcessor implements ArtifactConfigurationProcessor {

  @Override
  public ArtifactContext createArtifactContext(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {
    final String[] configResources = artifactContextConfiguration.getConfigResources();

    if (isEmpty(configResources)) {
      ((DefaultMuleContext) artifactContextConfiguration.getMuleContext())
          .setLifecycleStrategy(new NullDomainMuleContextLifecycleStrategy());
      return new ImmutableArtifactContext(artifactContextConfiguration.getMuleContext());
    }

    SpringXmlConfigurationBuilder springXmlConfigurationBuilder =
        new SpringXmlConfigurationBuilder(configResources,
                                          artifactContextConfiguration.getArtifactDeclaration(),
                                          artifactContextConfiguration.getArtifactProperties(),
                                          artifactContextConfiguration.getArtifactType(),
                                          artifactContextConfiguration.isEnableLazyInitialization(),
                                          artifactContextConfiguration.isDisableXmlValidations(),
                                          artifactContextConfiguration.getRuntimeLockFactory(),
                                          artifactContextConfiguration.getMemoryManagementService());
    artifactContextConfiguration.getParentArtifactContext()
        .ifPresent(parentContext -> springXmlConfigurationBuilder.setParentContext(parentContext.getMuleContext(),
                                                                                   parentContext.getArtifactAst()));
    artifactContextConfiguration.getServiceConfigurators().stream()
        .forEach(springXmlConfigurationBuilder::addServiceConfigurator);
    springXmlConfigurationBuilder.configure(artifactContextConfiguration.getMuleContext());
    return springXmlConfigurationBuilder.createArtifactContext();
  }
}
