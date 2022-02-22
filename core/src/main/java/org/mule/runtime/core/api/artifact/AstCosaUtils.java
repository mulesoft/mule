/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.artifact;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.core.api.util.boot.ExtensionLoaderUtils.getOptionalLoaderById;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_NAME_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_LOADER_ID;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_TYPE_LOADER_PROPERTY_NAME;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ArtifactType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.type.catalog.ApplicationTypeLoader;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;

public final class AstCosaUtils {

  private static final Logger LOGGER = getLogger(AstCosaUtils.class);

  public static ArtifactAst xx(List<String> configResources,
                               AstXmlParserSupplier parserSupplier,
                               Set<ExtensionModel> extensions,
                               ArtifactType artifactType,
                               boolean disableValidations,
                               MuleContext muleContext) {

    final ArtifactAst partialAst = doParseArtifactIntoAst(configResources, parserSupplier, extensions, true);

    return parseApplicationExtensionModel(partialAst, artifactType, muleContext)
        .map(extensionModel -> {
          Set<ExtensionModel> enrichedExtensionModels = new HashSet<>(extensions);
          enrichedExtensionModels.add(extensionModel);
          return doParseArtifactIntoAst(configResources, parserSupplier, enrichedExtensionModels, true); // TODO: use disableXmlValidations field
        }).orElseGet(() -> disableValidations ? partialAst : doParseArtifactIntoAst(configResources, parserSupplier, extensions, false));
  }

  private static Optional<ExtensionModel> parseApplicationExtensionModel(ArtifactAst ast,
                                                                         ArtifactType artifactType,
                                                                         MuleContext muleContext) {
    if (!artifactType.equals(APPLICATION)) {
      return empty();
    }

    Optional<ArtifactCoordinates> artifactCoordinates = muleContext.getConfiguration().getArtifactCoordinates();

    if (!artifactCoordinates.isPresent()) {
      logModelNotGenerated("No version specified on muleContext", muleContext);
      return empty();
    }

    Optional<ExtensionModelLoader> loader = getOptionalLoaderById(AstCosaUtils.class.getClassLoader(), MULE_SDK_LOADER_ID);
    if (loader.isPresent()) {
      final ExtensionManager extensionManager = muleContext.getExtensionManager();
      ExtensionModel appExtensionModel = loader.get()
          .loadExtensionModel(builder(muleContext.getExecutionClassLoader().getParent(),
              getDefault(extensionManager.getExtensions()))
              .addParameter("version", artifactCoordinates.get().getVersion())
              .addParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME, ast)
              .addParameter(MULE_SDK_EXTENSION_NAME_PROPERTY_NAME,
                  muleContext.getConfiguration().getId())
              .addParameter(MULE_SDK_TYPE_LOADER_PROPERTY_NAME, new ApplicationTypeLoader())
              .build());

      return of(appExtensionModel);
    } else {
      logModelNotGenerated("Mule ExtensionModelLoader not found", muleContext);
      return empty();
    }
  }

  private static ArtifactAst doParseArtifactIntoAst(List<String> configResources,
                                                    AstXmlParserSupplier parserSupplier,
                                                    Set<ExtensionModel> extensions,
                                                    boolean disableValidations) {
    try {
      return parserSupplier.getParser(extensions, disableValidations).parse(loadConfigResources(configResources));
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private static ConfigResource[] loadConfigResources(List<String> configs) throws ConfigurationException {
    try {
      ConfigResource[] artifactConfigResources = new ConfigResource[configs.size()];
      for (int i = 0; i < configs.size(); i++) {
        artifactConfigResources[i] = new ConfigResource(configs.get(i));
      }
      return artifactConfigResources;
    } catch (IOException e) {
      throw new ConfigurationException(e);
    }
  }

  private static void logModelNotGenerated(String reason, MuleContext muleContext) {
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn("ExtensionModel for app {} not generated: {}", muleContext.getConfiguration().getId(), reason);
    }
  }
}
