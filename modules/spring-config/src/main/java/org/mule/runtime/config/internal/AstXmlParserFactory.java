/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.util.AstTraversalDirection;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.StaticConfigurationPropertiesProvider;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.internal.exception.FilteredErrorTypeRepository;
import org.mule.runtime.dsl.api.xml.parser.ParsingPropertyResolver;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR;
import static org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;

public class AstXmlParserFactory {

  AstXmlParser createMuleXmlParser(Set<ExtensionModel> extensions,
                                   Map<String, String> artifactProperties, boolean disableXmlValidations,
                                   ArtifactType artifactType, ArtifactAst parentArtifactAst, boolean errorTypeRepository,
                                   FeatureFlaggingService featureFlaggingService) {
    ConfigurationPropertiesResolver propertyResolver =
        new DefaultConfigurationPropertiesResolver(empty(), new StaticConfigurationPropertiesProvider(artifactProperties));
    return createMuleXmlParser(extensions, disableXmlValidations, featureFlaggingService, AstXmlParser.builder(),
                               propertyKey -> (String) propertyResolver.resolveValue(propertyKey),
                               this.resolveParentArtifact(artifactType, parentArtifactAst, errorTypeRepository));
  }

  AstXmlParser createMuleXmlParser(Set<ExtensionModel> extensions,
                                   boolean disableXmlValidations,
                                   FeatureFlaggingService featureFlaggingService, Builder builder,
                                   ParsingPropertyResolver parsingPropertyResolver, ArtifactAst parentArtifact) {
    builder.withPropertyResolver(parsingPropertyResolver)
        // TODO MULE-19203 for policies this includes all extensions from the app as well. It should be just the ones
        // declared in the policy, with a feature flag for getting the ones from the app as well (ref:
        // MuleSystemProperties#SHARE_ERROR_TYPE_REPOSITORY_PROPERTY).
        .withExtensionModels(extensions)
        .withParentArtifact(parentArtifact);
    if (!featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)) {
      builder.withLegacyFailStrategy();
    }
    if (disableXmlValidations) {
      builder.withSchemaValidationsDisabled();
    }
    return builder.build();
  }

  protected ArtifactAst resolveParentArtifact(ArtifactType artifactType, final ArtifactAst parentArtifactAst,
                                              boolean errorTypeRepository) {
    if (POLICY.equals(artifactType)) {
      if (errorTypeRepository) {
        // Because MULE-18196 breaks backwards, we need this feature flag to allow legacy behavior
        return parentArtifactAst;
      } else {
        return new ArtifactAst() {

          @Override
          public Set<ExtensionModel> dependencies() {
            return parentArtifactAst.dependencies();
          }

          @Override
          public Optional<ArtifactAst> getParent() {
            return parentArtifactAst.getParent();
          }

          @Override
          public Stream<ComponentAst> recursiveStream(AstTraversalDirection direction) {
            return parentArtifactAst.recursiveStream(direction);
          }

          @Override
          public Spliterator<ComponentAst> recursiveSpliterator(AstTraversalDirection direction) {
            return parentArtifactAst.recursiveSpliterator(direction);
          }

          @Override
          public Stream<ComponentAst> topLevelComponentsStream() {
            return parentArtifactAst.topLevelComponentsStream();
          }

          @Override
          public Spliterator<ComponentAst> topLevelComponentsSpliterator() {
            return parentArtifactAst.topLevelComponentsSpliterator();
          }

          @Override
          public void updatePropertiesResolver(UnaryOperator<String> newPropertiesResolver) {
            parentArtifactAst.updatePropertiesResolver(newPropertiesResolver);
          }

          @Override
          public ErrorTypeRepository getErrorTypeRepository() {
            // Since there is already a workaround to allow polices to use http connector without declaring the dependency
            // and relying on it provided by the app, this case has to be accounted for here when handling error codes as
            // well.
            return new FilteredErrorTypeRepository(parentArtifactAst.getErrorTypeRepository(), singleton("HTTP"));
          }
        };
      }
    }
    return parentArtifactAst;
  }
}
