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
import static org.mule.runtime.extension.api.util.XmlModelUtils.createXmlLanguageModel;
import static org.mule.runtime.internal.dsl.DslConstants.THIS_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.THIS_PREFIX;

import static java.util.Collections.singleton;
import static java.util.Optional.of;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.artifact.activation.internal.ast.ArtifactExtensionModelsEnricher;
import org.mule.runtime.module.extension.mule.internal.loader.BaseExtensionModelDecorator;
import org.mule.runtime.module.extension.mule.internal.loader.MuleSdkExtensionExtensionModelLoader;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ArtifactExtensionModelsEnricher} that loads an {@link ExtensionModel} from an extension's {@link ArtifactAst} and
 * decorates it so that the prefix and namespace correspond to {@link THIS_PREFIX} and {@link THIS_NAMESPACE} respectively.
 *
 * @since 4.5.0
 */
public class MuleSdkLocalExtensionModelsEnricher implements ArtifactExtensionModelsEnricher {

  private static final Set<ComponentType> REUSABLE_COMPONENT_TYPES = singleton(OPERATION_DEF);
  private final String version;

  public MuleSdkLocalExtensionModelsEnricher(String version) {
    this.version = version;
  }

  @Override
  public boolean applicable(ArtifactAst ast) {
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
        .build());

    // Decorates the model so that the prefix and namespaces are changed to the artifact-local ones.
    ExtensionModel decoratedExtensionModel = new MuleSdkLocalExtensionModelDecorator(extensionModel);

    // Enriches the ExtensionModels by adding the new one.
    Set<ExtensionModel> enrichedExtensionModels = new HashSet<>(extensions);
    enrichedExtensionModels.add(decoratedExtensionModel);
    return enrichedExtensionModels;
  }

  private static class MuleSdkLocalExtensionModelDecorator extends BaseExtensionModelDecorator {

    public MuleSdkLocalExtensionModelDecorator(ExtensionModel extensionModel) {
      super(extensionModel);
    }

    @Override
    public XmlDslModel getXmlDslModel() {
      return createXmlLanguageModel(of(THIS_PREFIX), of(THIS_NAMESPACE), getName(), getVersion());
    }
  }
}
