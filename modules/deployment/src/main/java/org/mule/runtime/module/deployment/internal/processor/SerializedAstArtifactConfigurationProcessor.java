/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.processor;

import static org.mule.runtime.api.util.MuleSystemProperties.FORCE_PARSE_CONFIG_XMLS_ON_DEPLOYMENT;

import static java.lang.Boolean.getBoolean;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptySet;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.serialization.ArtifactAstDeserializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializerProvider;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;

import java.util.Set;

/**
 * Implementation of {@link ArtifactConfigurationProcessor} that reads the serialized AST available in the artifact and delegates
 * to {@link ArtifactAstConfigurationBuilder} to create registry and populate the {@link MuleContext}.
 * 
 * @since 4.5
 */
public class SerializedAstArtifactConfigurationProcessor extends AbstractAstConfigurationProcessor {

  public static final String SERIALIZED_ARTIFACT_AST_LOCATION = "META-INF/mule-artifact/artifact.ast";

  private final ArtifactAstDeserializer defaultArtifactAstDeserializer = new ArtifactAstSerializerProvider().getDeserializer();

  @Override
  protected ArtifactAst obtainArtifactAst(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {
    try {
      return defaultArtifactAstDeserializer
          .deserialize(currentThread().getContextClassLoader().getResourceAsStream(SERIALIZED_ARTIFACT_AST_LOCATION),
                       name -> getExtensions(artifactContextConfiguration.getMuleContext().getExtensionManager())
                           .stream()
                           .filter(x -> x.getName().equals(name))
                           .findFirst()
                           .orElse(null));
    } catch (Exception e) {
      throw new ConfigurationException(e);
    }
  }

  private Set<ExtensionModel> getExtensions(ExtensionManager extensionManager) {
    return extensionManager == null ? emptySet() : extensionManager.getExtensions();
  }

  public static ArtifactConfigurationProcessor serializedAstWithFallbackArtifactConfigurationProcessor() {
    if (getBoolean(FORCE_PARSE_CONFIG_XMLS_ON_DEPLOYMENT)) {
      return new AstXmlParserArtifactConfigurationProcessor();
    } else {
      return new FallbackArtifactConfigurationProcessor(new SerializedAstArtifactConfigurationProcessor(),
                                                        new AstXmlParserArtifactConfigurationProcessor());
    }
  }
}
