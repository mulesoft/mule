/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION_DEF;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import static java.util.Collections.singleton;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.artifact.activation.internal.ast.ArtifactExtensionModelsEnricher;
import org.mule.runtime.module.extension.mule.internal.loader.MuleSdkExtensionExtensionModelLoader;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link ArtifactExtensionModelsEnricher} that loads an {@link ExtensionModel} from an extension's {@link ArtifactAst}.
 *
 * @since 4.5.0
 */
public class MuleSdkExtensionExtensionModelsEnricher implements ArtifactExtensionModelsEnricher {

  private static final Set<ComponentType> REUSABLE_COMPONENT_TYPES = singleton(OPERATION_DEF);

  private final Map<String, Object> extraParameters;
  private final String version;

  /**
   * Creates a new enricher with the given parameters.
   *
   * @param version         the artifact's version.
   * @param extraParameters allows for adding extra parameters to the loading request for the new model.
   */
  public MuleSdkExtensionExtensionModelsEnricher(String version, Map<String, Object> extraParameters) {
    this.extraParameters = extraParameters;
    this.version = version;
  }

  @Override
  public boolean isApplicable(ArtifactAst ast) {
    return ast.topLevelComponents().size() == 1 &&
        ast.topLevelComponents().get(0).directChildrenStream()
            .anyMatch(component -> REUSABLE_COMPONENT_TYPES.contains(component.getComponentType()));
  }

  @Override
  public Set<ExtensionModel> getEnrichedExtensionModels(ArtifactAst ast, ClassLoader classLoader,
                                                        Set<ExtensionModel> extensions) {
    // Loads the model using the Mule SDK Extensions loader.
    ExtensionModelLoader loader = new MuleSdkExtensionExtensionModelLoader();
    ExtensionModel extensionModel = loader.loadExtensionModel(builder(classLoader, getDefault(extensions))
        .addParameter(VERSION_PROPERTY_NAME, version)
        .addParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME, ast)
        .addParameters(extraParameters)
        .build());

    // Enriches the ExtensionModels by adding the new one.
    Set<ExtensionModel> enrichedExtensionModels = new HashSet<>(extensions);
    enrichedExtensionModels.add(extensionModel);
    return enrichedExtensionModels;
  }
}
