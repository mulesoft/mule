/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser.ast;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION_DEF;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.artifact.activation.internal.ast.ArtifactExtensionModelParser;
import org.mule.runtime.module.extension.mule.internal.loader.MuleSdkExtensionExtensionModelLoader;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * {@link ArtifactExtensionModelParser} that loads an {@link ExtensionModel} from an extension's {@link ArtifactAst}.
 *
 * @since 4.5.0
 */
public class MuleSdkExtensionArtifactExtensionModelParser implements ArtifactExtensionModelParser {

  private static final Set<ComponentType> REUSABLE_COMPONENT_TYPES = singleton(OPERATION_DEF);

  private final Consumer<ExtensionModel> onNewExtensionModel;
  private final String version;

  /**
   * Creates a new parser with the given parameters.
   *
   * @param version             the artifact's version.
   * @param onNewExtensionModel a consumer to call when the parser creates the artifact's {@link ExtensionModel}.
   */
  public MuleSdkExtensionArtifactExtensionModelParser(String version, Consumer<ExtensionModel> onNewExtensionModel) {
    this.onNewExtensionModel = onNewExtensionModel;
    this.version = version;
  }

  @Override
  public Optional<ExtensionModel> parseArtifactExtensionModel(ArtifactAst ast, ClassLoader classLoader,
                                                              Set<ExtensionModel> extensions) {
    if (!containsReusableComponents(ast)) {
      return empty();
    }

    // Loads the model using the Mule SDK Extensions loader.
    ExtensionModelLoader loader = new MuleSdkExtensionExtensionModelLoader();
    ExtensionModel extensionModel = loader.loadExtensionModel(builder(classLoader, getDefault(extensions))
        .addParameter(VERSION_PROPERTY_NAME, version)
        .addParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME, ast)
        .build());

    // Calls the registered consumer with the new ExtensionModel.
    onNewExtensionModel.accept(extensionModel);

    return of(extensionModel);
  }

  private boolean containsReusableComponents(ArtifactAst ast) {
    return ast.topLevelComponents().size() == 1 &&
        ast.topLevelComponents().get(0).directChildrenStream()
            .anyMatch(component -> REUSABLE_COMPONENT_TYPES.contains(component.getComponentType()));
  }
}
