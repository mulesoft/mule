/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.processor;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;
import org.slf4j.Logger;

import static java.lang.Boolean.getBoolean;
import static org.mule.runtime.api.util.MuleSystemProperties.FORCE_PARSE_CONFIG_XMLS_ON_DEPLOYMENT_PROPERTY;
import static org.slf4j.LoggerFactory.getLogger;

public class VoltronArtifactConfigurationProcessor extends SerializedAstArtifactConfigurationProcessor {

  private static final Logger LOGGER = getLogger(VoltronArtifactConfigurationProcessor.class);

  private final ArtifactAst artifactAst;

  public VoltronArtifactConfigurationProcessor(ArtifactAst artifactAst) {
    this.artifactAst = artifactAst;
  }

  @Override
  protected ArtifactAst obtainArtifactAst(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {
    return this.artifactAst;
  }
}
