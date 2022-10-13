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
import org.mule.runtime.module.extension.mule.internal.loader.MuleSdkPluginExtensionModelLoader;

import java.util.Set;

/**
 * {@link MuleSdkExtensionModelLoadingMediator} that loads an {@link ExtensionModel} from a plugin's {@link ArtifactAst}.
 *
 * @since 4.5.0
 */
public class MuleSdkPluginExtensionModelLoadingMediator extends AbstractMuleSdkExtensionModelLoadingMediator {

  private static final Set<ComponentType> REUSABLE_COMPONENT_TYPES = singleton(OPERATION_DEF);

  private final ArtifactCoordinates artifactCoordinates;

  /**
   * Creates a new helper with the given parameters.
   *
   * @param artifactCoordinates the artifact's coordinates.
   */
  public MuleSdkPluginExtensionModelLoadingMediator(ArtifactCoordinates artifactCoordinates) {
    this.artifactCoordinates = artifactCoordinates;
  }

  @Override
  protected ArtifactCoordinates getArtifactCoordinates() throws ConfigurationException {
    return artifactCoordinates;
  }

  @Override
  protected ExtensionModelLoader getLoader() throws ConfigurationException {
    return new MuleSdkPluginExtensionModelLoader();
  }

  @Override
  protected boolean containsReusableComponents(ArtifactAst ast) {
    return ast.topLevelComponents().size() == 1 &&
        ast.topLevelComponents().get(0).directChildrenStream()
            .anyMatch(component -> REUSABLE_COMPONENT_TYPES.contains(component.getComponentType()));
  }
}
