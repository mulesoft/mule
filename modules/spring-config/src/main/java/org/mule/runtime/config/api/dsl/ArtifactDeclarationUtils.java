/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl;

import static java.util.Optional.empty;
import static org.mule.runtime.ast.api.ComponentAst.BODY_RAW_PARAM_NAME;
import static org.mule.runtime.ast.api.ComponentMetadataAst.EMPTY_METADATA;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.builder.ArtifactAstBuilder;
import org.mule.runtime.ast.api.builder.ComponentAstBuilder;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Utilities to convert between {@link ArtifactDeclaration} and {@link ArtifactAst}.
 *
 * @since 4.4
 */
public final class ArtifactDeclarationUtils {

  private ArtifactDeclarationUtils() {
    // Nothing to do
  }

  /**
   * Generated a new {@link ArtifactAst} based on the provided {@link ArtifactDeclaration}.
   *
   * @param artifactDeclaration the declaration to generate the {@link ArtifactAst} for.
   * @param extensionModels the models of the extensions used in the app modeled by the provided declarer.
   * @return the generated AST.
   */
  public static ArtifactAst toArtifactast(ArtifactDeclaration artifactDeclaration, Set<ExtensionModel> extensionModels) {
    final ArtifactAstBuilder astBuilder =
        ArtifactAstBuilder.builder(extensionModels, empty());

    convertArtifactDeclarationToComponentModel(extensionModels, artifactDeclaration, astBuilder);
    return astBuilder.build();
  }

  private static void convertArtifactDeclarationToComponentModel(Set<ExtensionModel> extensionModels,
                                                                 ArtifactDeclaration artifactDeclaration,
                                                                 ArtifactAstBuilder astBuilder) {
    if (artifactDeclaration != null && !extensionModels.isEmpty()) {
      ExtensionModel muleModel = MuleExtensionModelProvider.getExtensionModel();
      if (!extensionModels.contains(muleModel)) {
        extensionModels = new HashSet<>(extensionModels);
        extensionModels.add(muleModel);
      }

      DslElementModelFactory elementFactory = DslElementModelFactory.getDefault(DslResolvingContext.getDefault(extensionModels));

      artifactDeclaration.getGlobalElements().stream()
          .map(e -> elementFactory.create((ElementDeclaration) e))
          .filter(Optional::isPresent)
          .map(e -> e.get().getConfiguration())
          .forEach(config -> config
              .ifPresent(c -> convertComponentConfiguration(c, astBuilder.addTopLevelComponent())));
    }
  }

  private static void convertComponentConfiguration(ComponentConfiguration componentConfiguration,
                                                    ComponentAstBuilder componentAstBuilder) {
    componentAstBuilder
        .withIdentifier(componentConfiguration.getIdentifier())
        .withMetadata(EMPTY_METADATA);
    for (Map.Entry<String, String> parameter : componentConfiguration.getParameters().entrySet()) {
      componentAstBuilder.withRawParameter(parameter.getKey(), parameter.getValue());
    }
    componentConfiguration.getValue().ifPresent(value -> componentAstBuilder.withRawParameter(BODY_RAW_PARAM_NAME, value));
    for (ComponentConfiguration childComponentConfiguration : componentConfiguration.getNestedComponents()) {
      convertComponentConfiguration(childComponentConfiguration, componentAstBuilder.addChildComponent());
    }
  }
}
