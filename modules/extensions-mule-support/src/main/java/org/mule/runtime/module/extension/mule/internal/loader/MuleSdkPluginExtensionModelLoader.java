/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_LOADER_ID;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.AbstractExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkPluginExtensionModelParserFactory;

import java.util.Optional;

/**
 * {@link ExtensionModelLoader} implementation for Mule SDK Plugins.
 *
 * @since 4.5.0
 */
public class MuleSdkPluginExtensionModelLoader extends AbstractExtensionModelLoader {

  private static final String MULE_SDK_EXTENSION_MODEL_PROPERTY_NAME = "_muleSdkArtifactExtensionModel";

  private final MuleSdkPluginExtensionModelParserFactory parserFactory;

  public MuleSdkPluginExtensionModelLoader() {
    parserFactory = new MuleSdkPluginExtensionModelParserFactory();
  }

  @Override
  public String getId() {
    return MULE_SDK_EXTENSION_LOADER_ID;
  }

  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    super.configureContextBeforeDeclaration(context);
    // Takes care of doing preparations of the context which are necessary by the parser.
    // Registers a callback in case the parser discovers the ExtensionModel as part of the process.
    parserFactory.configureContextBeforeParsing(context, extensionModel -> context
        .addParameter(MULE_SDK_EXTENSION_MODEL_PROPERTY_NAME, extensionModel));
  }

  @Override
  protected ExtensionModelParserFactory getExtensionModelParserFactory(ExtensionLoadingContext context) {
    return parserFactory;
  }

  @Override
  public ExtensionModel loadExtensionModel(ExtensionLoadingContext context, ArtifactCoordinates artifactCoordinates) {
    // The extension model may have been already created and placed in the context as a parameter.
    Optional<ExtensionModel> extensionModel = context.getParameter(MULE_SDK_EXTENSION_MODEL_PROPERTY_NAME);
    return extensionModel.orElseGet(() -> super.loadExtensionModel(context, artifactCoordinates));
  }
}
