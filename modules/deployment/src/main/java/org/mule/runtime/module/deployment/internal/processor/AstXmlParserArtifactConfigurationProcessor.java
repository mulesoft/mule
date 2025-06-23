/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.processor;

import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.config.internal.ApplicationFilteredFromPolicyArtifactAst.applicationFilteredFromPolicyArtifactAst;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.module.artifact.activation.api.ast.ArtifactAstUtils.parseAndBuildAppExtensionModel;

import static java.util.Collections.emptySet;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesHierarchyBuilder;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.core.internal.registry.Registry;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;

import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link ArtifactConfigurationProcessor} that parses the XML configuration files and delegates to
 * {@link ArtifactAstConfigurationBuilder} to create registry and populate the {@link MuleContext}.
 *
 * @since 4.5
 */
public final class AstXmlParserArtifactConfigurationProcessor extends AbstractAstConfigurationProcessor {

  @Override
  protected ArtifactAst obtainArtifactAst(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {
    return createApplicationModel(artifactContextConfiguration.getMuleContext(),
                                  artifactContextConfiguration.getConfigResources(),
                                  artifactContextConfiguration.getArtifactProperties(),
                                  artifactContextConfiguration.getArtifactType(),
                                  artifactContextConfiguration.getParentArtifactContext()
                                      .map(ArtifactContext::getArtifactAst)
                                      .orElse(emptyArtifact()),
                                  artifactContextConfiguration.isDisableXmlValidations(),
                                  artifactContextConfiguration.getExpressionLanguageMetadataService());
  }

  private Set<ExtensionModel> getExtensions(ExtensionManager extensionManager) {
    return extensionManager == null ? emptySet() : extensionManager.getExtensions();
  }

  private ArtifactAst createApplicationModel(MuleContext muleContext,
                                             String[] artifactConfigResources,
                                             Map<String, String> artifactProperties,
                                             ArtifactType artifactType,
                                             ArtifactAst parentArtifactAst,
                                             boolean disableXmlValidations,
                                             ExpressionLanguageMetadataService expressionLanguageMetadataService)
      throws ConfigurationException {

    Set<ExtensionModel> extensions = getExtensions(muleContext.getExtensionManager());

    try {
      final ArtifactAst artifactAst;

      if (artifactConfigResources.length == 0) {
        artifactAst = emptyArtifact();
      } else {
        artifactAst = parseAndBuildAppExtensionModel(muleContext.getConfiguration().getId(),
                                                     artifactConfigResources,
                                                     (exts, disableValidations) -> createMuleXmlParser(muleContext, exts,
                                                                                                       artifactProperties,
                                                                                                       artifactType,
                                                                                                       parentArtifactAst,
                                                                                                       disableValidations),
                                                     extensions,
                                                     disableXmlValidations,
                                                     muleContext.getExecutionClassLoader(),
                                                     muleContext.getConfiguration(),
                                                     expressionLanguageMetadataService);
      }

      return artifactAst;
    } catch (ConfigurationException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigurationException(e);
    }
  }

  private AstXmlParser createMuleXmlParser(MuleContext muleContext,
                                           Set<ExtensionModel> extensions,
                                           Map<String, String> artifactProperties,
                                           ArtifactType artifactType,
                                           ArtifactAst parentArtifactAst,
                                           boolean disableXmlValidations) {
    ConfigurationPropertiesResolver propertyResolver = new ConfigurationPropertiesHierarchyBuilder()
        .withApplicationProperties(artifactProperties)
        .build();

    FeatureFlaggingService featureFlaggingService = getFeatureFlaggingService(muleContext);
    Builder builder = AstXmlParser.builder()
        .withPropertyResolver(propertyKey -> (String) propertyResolver.resolveValue(propertyKey))
        .withExtensionModels(extensions)
        .withArtifactType(toAstArtifactType(artifactType))
        .withParentArtifact(resolveParentArtifact(parentArtifactAst, artifactType, featureFlaggingService));
    if (!featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)) {
      builder.withLegacyFailStrategy();
    }
    if (disableXmlValidations) {
      builder.withSchemaValidationsDisabled();
    }

    return builder.build();
  }

  private org.mule.runtime.ast.api.ArtifactType toAstArtifactType(ArtifactType artifactType) {
    switch (artifactType) {
      case APP:
        return org.mule.runtime.ast.api.ArtifactType.APPLICATION;
      case DOMAIN:
        return org.mule.runtime.ast.api.ArtifactType.DOMAIN;
      case POLICY:
        return org.mule.runtime.ast.api.ArtifactType.POLICY;
      default:
        throw new IllegalArgumentException("The provided artifact type '" + artifactType + "' cannot be deployed.");
    }
  }

  private FeatureFlaggingService getFeatureFlaggingService(MuleContext muleContext) {
    Registry originalRegistry = ((MuleRegistryHelper) (((MuleContextWithRegistry) muleContext).getRegistry())).getDelegate();
    return originalRegistry.lookupObject(FEATURE_FLAGGING_SERVICE_KEY);
  }

  private ArtifactAst resolveParentArtifact(ArtifactAst parentArtifactAst, ArtifactType artifactType,
                                            FeatureFlaggingService featureFlaggingService) {
    if (POLICY.equals(artifactType)) {
      return applicationFilteredFromPolicyArtifactAst(parentArtifactAst, featureFlaggingService);
    }
    return parentArtifactAst;
  }

}
