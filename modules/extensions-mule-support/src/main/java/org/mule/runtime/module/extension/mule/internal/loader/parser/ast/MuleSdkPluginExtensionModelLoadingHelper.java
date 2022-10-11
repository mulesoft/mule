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

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.artifact.activation.internal.ast.AbstractMuleSdkExtensionModelLoadingHelper;
import org.mule.runtime.module.artifact.activation.internal.ast.MuleSdkExtensionModelLoadingHelper;
import org.mule.runtime.module.extension.mule.internal.loader.MuleSdkPluginExtensionModelLoader;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * {@link MuleSdkExtensionModelLoadingHelper} that loads an {@link ExtensionModel} from a plugin's {@link ArtifactAst}.
 *
 * @since 4.5.0
 */
public class MuleSdkPluginExtensionModelLoadingHelper extends AbstractMuleSdkExtensionModelLoadingHelper {

  private static final Set<ComponentType> REUSABLE_COMPONENT_TYPES = singleton(OPERATION_DEF);

  private final Consumer<ExtensionModel> onNewExtensionModel;
  private final String version;

  /**
   * Creates a new helper with the given parameters.
   *
   * @param version             the artifact's version.
   * @param onNewExtensionModel a consumer to call when the helper creates the artifact's {@link ExtensionModel}.
   */
  public MuleSdkPluginExtensionModelLoadingHelper(String version, Consumer<ExtensionModel> onNewExtensionModel) {
    this.onNewExtensionModel = onNewExtensionModel;
    this.version = version;
  }

  @Override
  public Optional<ExtensionModel> loadExtensionModel(ArtifactAst ast, ClassLoader classLoader,
                                                     Set<ExtensionModel> extensions)
      throws ConfigurationException {
    Optional<ExtensionModel> newExtensionModel = super.loadExtensionModel(ast, classLoader, extensions);
    // Calls the registered consumer with the new ExtensionModel (if any).
    newExtensionModel.ifPresent(onNewExtensionModel);
    return newExtensionModel;
  }

  @Override
  protected String getVersion() throws ConfigurationException {
    return version;
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
