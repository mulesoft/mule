/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.processor;

import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.SERIALIZED_ARTIFACT_AST_LOCATION;

import static java.lang.String.format;
import static java.util.Collections.emptySet;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.serialization.ArtifactAstDeserializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializerProvider;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.slf4j.Logger;

/**
 * Implementation of {@link ArtifactConfigurationProcessor} that reads the serialized AST available in the artifact and delegates
 * to {@link ArtifactAstConfigurationBuilder} to create registry and populate the {@link MuleContext}.
 *
 * @since 4.5
 */
public class SerializedAstArtifactConfigurationProcessor extends AbstractAstConfigurationProcessor
    implements FallbackAllowedArtifactConfigurationProcessor {

  private static final Logger LOGGER = getLogger(FallbackArtifactConfigurationProcessor.class);

  private final ArtifactAstDeserializer defaultArtifactAstDeserializer = new ArtifactAstSerializerProvider().getDeserializer();

  @Override
  public boolean check(ArtifactContextConfiguration artifactContextConfiguration) {
    MuleContext muleContext = artifactContextConfiguration.getMuleContext();
    try (InputStream seralizedAstStream = muleContext.getExecutionClassLoader()
        .getResourceAsStream(SERIALIZED_ARTIFACT_AST_LOCATION)) {
      if (seralizedAstStream == null) {
        LOGGER.info("Serialized AST not avaliable for artifact '"
            + muleContext.getConfiguration().getId() + "'");
      }

      return seralizedAstStream != null;
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  protected ArtifactAst obtainArtifactAst(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {
    try {
      MuleContext muleContext = artifactContextConfiguration.getMuleContext();
      try (InputStream inputStream = muleContext.getExecutionClassLoader()
          .getResourceAsStream(SERIALIZED_ARTIFACT_AST_LOCATION)) {

        ArtifactAst deserializedArtifactAst = defaultArtifactAstDeserializer
            .deserialize(inputStream,
                         name -> getExtensions(muleContext.getExtensionManager())
                             .stream()
                             .filter(x -> x.getName().equals(name))
                             .findFirst()
                             .orElse(null));

        if (!isEquivalentAstArtifactType(artifactContextConfiguration.getArtifactType(),
                                         deserializedArtifactAst.getArtifactType())) {
          throw new IllegalStateException(format("Expected artifact type '%s' but serialized ast was '%s'",
                                                 artifactContextConfiguration.getArtifactType(),
                                                 deserializedArtifactAst.getArtifactType()));
        }

        return deserializedArtifactAst;
      }
    } catch (Exception e) {
      throw new ConfigurationException(e);
    }
  }

  private boolean isEquivalentAstArtifactType(ArtifactType artifactType, org.mule.runtime.ast.api.ArtifactType astArtifactType) {
    switch (artifactType) {
      case APP:
        return astArtifactType == org.mule.runtime.ast.api.ArtifactType.APPLICATION;
      case DOMAIN:
        return astArtifactType == org.mule.runtime.ast.api.ArtifactType.DOMAIN;
      case POLICY:
        return astArtifactType == org.mule.runtime.ast.api.ArtifactType.POLICY;
      default:
        throw new IllegalArgumentException("The provided artifact type '" + artifactType + "' cannot be deployed.");
    }
  }

  private Set<ExtensionModel> getExtensions(ExtensionManager extensionManager) {
    return extensionManager == null ? emptySet() : extensionManager.getExtensions();
  }

  public static ArtifactConfigurationProcessor serializedAstWithFallbackArtifactConfigurationProcessor() {
    return new FallbackArtifactConfigurationProcessor(new SerializedAstArtifactConfigurationProcessor(),
                                                      new AstXmlParserArtifactConfigurationProcessor());
  }
}
