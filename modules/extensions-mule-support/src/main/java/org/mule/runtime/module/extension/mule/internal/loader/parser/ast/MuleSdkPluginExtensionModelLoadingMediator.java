/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser.ast;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION_DEF;

import static java.util.Collections.singleton;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.artifact.activation.internal.ast.AbstractMuleSdkExtensionModelLoadingMediator;
import org.mule.runtime.module.artifact.activation.internal.ast.MuleSdkExtensionModelLoadingMediator;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * {@link MuleSdkExtensionModelLoadingMediator} that loads an {@link ExtensionModel} from a plugin's {@link ArtifactAst}.
 *
 * @since 4.5.0
 */
public class MuleSdkPluginExtensionModelLoadingMediator extends AbstractMuleSdkExtensionModelLoadingMediator {

  private static final Set<ComponentType> REUSABLE_COMPONENT_TYPES = singleton(OPERATION_DEF);

  private final String version;
  private final ExtensionModelLoader extensionModelLoader;
  private final Consumer<ExtensionModel> onNewExtensionModel;

  /**
   * Creates a new mediator with the given parameters.
   *
   * @param artifactCoordinates  the artifact's coordinates.
   * @param version              the artifact's version in case the full coordinates are not available.
   * @param extensionModelLoader the loader to use for loading the {@link ExtensionModel}.
   * @param onNewExtensionModel  a consumer to call if the artifact's {@link ExtensionModel} is created as part of the parsing
   *                             process.
   */
  public MuleSdkPluginExtensionModelLoadingMediator(Optional<ArtifactCoordinates> artifactCoordinates,
                                                    String version,
                                                    ExtensionModelLoader extensionModelLoader,
                                                    Consumer<ExtensionModel> onNewExtensionModel) {
    super(artifactCoordinates);
    this.version = version;
    this.extensionModelLoader = extensionModelLoader;
    this.onNewExtensionModel = onNewExtensionModel;
  }

  @Override
  public Optional<ExtensionModel> loadExtensionModel(ArtifactAst ast, ClassLoader classLoader, Set<ExtensionModel> extensions)
      throws ConfigurationException {
    Optional<ExtensionModel> extensionModel = super.loadExtensionModel(ast, classLoader, extensions);
    extensionModel.ifPresent(onNewExtensionModel);
    return extensionModel;
  }

  @Override
  protected String getVersion() throws ConfigurationException {
    return artifactCoordinates.map(ArtifactCoordinates::getVersion).orElse(version);
  }

  @Override
  protected ExtensionModelLoader getLoader() throws ConfigurationException {
    return extensionModelLoader;
  }

  @Override
  protected boolean containsReusableComponents(ArtifactAst ast) {
    return ast.topLevelComponents().size() == 1 &&
        ast.topLevelComponents().get(0).directChildrenStream()
            .anyMatch(component -> REUSABLE_COMPONENT_TYPES.contains(component.getComponentType()));
  }
}
