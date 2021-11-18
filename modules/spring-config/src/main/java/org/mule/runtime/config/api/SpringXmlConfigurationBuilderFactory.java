/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.internal.config.RuntimeLockFactoryUtil.getRuntimeLockFactory;

import static java.util.Collections.emptyMap;

import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.config.internal.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.internal.memory.management.DefaultMemoryManagementService;

import java.util.Map;

/**
 * @since 4.0
 * @deprecated Use {@link ArtifactAstConfigurationBuilder} instead.
 */
@Deprecated
public final class SpringXmlConfigurationBuilderFactory {

  private SpringXmlConfigurationBuilderFactory() {
    // Nothing to do
  }

  // TODO: MULE-19422 Remove specific tests usages
  @Deprecated
  public static ConfigurationBuilder createConfigurationBuilder(String configResource) throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(configResource);
  }

  // TODO: MULE-19422 Remove specific tests usages
  @Deprecated
  public static ConfigurationBuilder createConfigurationBuilder(String configResource, boolean lazyInit)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(new String[] {configResource}, lazyInit, true);
  }

  // TODO: MULE-19422 Remove specific tests usages
  @Deprecated
  public static ConfigurationBuilder createConfigurationBuilder(String configResource, boolean lazyInit,
                                                                boolean disableXmlValidations)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(new String[] {configResource}, lazyInit, disableXmlValidations);
  }

  // TODO: MULE-19422 Remove specific tests usages
  @Deprecated
  public static ConfigurationBuilder createConfigurationBuilder(String[] configResources, boolean lazyInit)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(configResources, lazyInit, true);
  }

  // TODO: MULE-19422 Remove testing infrastructure usages
  @Deprecated
  public static ConfigurationBuilder createConfigurationBuilder(ArtifactDeclaration artifactDeclaration)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(new String[0], artifactDeclaration, emptyMap(), APP, false, false,
                                             getRuntimeLockFactory());
  }

  // TODO: MULE-19422 Remove testing infrastructure usages
  @Deprecated
  public static ConfigurationBuilder createConfigurationBuilder(String[] configResources, ArtifactContext domainArtifactContext)
      throws ConfigurationException {
    final SpringXmlConfigurationBuilder springXmlConfigurationBuilder =
        new SpringXmlConfigurationBuilder(configResources, emptyMap(), APP, false, false);
    if (domainArtifactContext != null) {
      springXmlConfigurationBuilder.setParentContext(domainArtifactContext.getMuleContext(),
                                                     domainArtifactContext.getArtifactAst());
    }
    return springXmlConfigurationBuilder;
  }

  @Deprecated
  public static ConfigurationBuilder createConfigurationBuilder(String configResource, Map<String, String> artifactProperties,
                                                                ArtifactType artifactType)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(configResource, artifactProperties, artifactType);
  }

  // Prod code usage
  public static ConfigurationBuilder createConfigurationBuilder(String[] configResources, Map<String, String> artifactProperties,
                                                                ArtifactType artifactType)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(configResources, artifactProperties, artifactType, false, false);
  }

  // TODO: MULE-19422 Remove testing infrastructure usages
  @Deprecated
  public static ConfigurationBuilder createConfigurationBuilder(String configResource, Map<String, String> artifactProperties,
                                                                ArtifactType artifactType, boolean enableLazyInit,
                                                                boolean disableXmlValidations)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(new String[] {configResource}, artifactProperties, artifactType, enableLazyInit,
                                             disableXmlValidations);
  }

  // TODO: MULE-19422 Remove testing infrastructure usages
  @Deprecated
  public static ConfigurationBuilder createConfigurationBuilder(String[] configResources, Map<String, String> artifactProperties,
                                                                ArtifactType artifactType, boolean enableLazyInit,
                                                                boolean disableXmlValidations)
      throws ConfigurationException {
    return new SpringXmlConfigurationBuilder(configResources, artifactProperties, artifactType, enableLazyInit,
                                             disableXmlValidations);
  }
}
