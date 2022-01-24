/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.processor;

import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;

public class SerializedAstArtifactConfigurationProcessor implements ArtifactConfigurationProcessor {

  @Override
  public ArtifactContext createArtifactContext(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {
    throw new ConfigurationException(I18nMessageFactory.createStaticMessage("Not yet implemented"));
  }

  public static ArtifactConfigurationProcessor serializedAstWithFallbackArtifactConfigurationProcessor() {
    return new FallbackArtifactConfigurationProcessor(new SerializedAstArtifactConfigurationProcessor(),
                                                      new AstArtifactConfigurationProcessor());
  }
}
