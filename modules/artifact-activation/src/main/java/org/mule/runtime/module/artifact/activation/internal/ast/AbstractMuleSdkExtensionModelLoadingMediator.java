/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.ast;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;

import java.util.Optional;
import java.util.Set;

/**
 * Template implementation for {@link MuleSdkExtensionModelLoadingMediator}.
 *
 * @since 4.5.0
 */
public abstract class AbstractMuleSdkExtensionModelLoadingMediator implements MuleSdkExtensionModelLoadingMediator {

  @Override
  public Optional<ExtensionModel> loadExtensionModel(ArtifactAst ast, ClassLoader classLoader,
                                                     Set<ExtensionModel> extensions)
      throws ConfigurationException {
    if (!containsReusableComponents(ast)) {
      return empty();
    }

    ArtifactCoordinates artifactCoordinates = getArtifactCoordinates();
    ExtensionModelLoader loader = getLoader();

    ExtensionModelLoadingRequest.Builder loadingRequestBuilder = builder(classLoader, getDefault(extensions))
        .setArtifactCoordinates(artifactCoordinates)
        .addParameter(VERSION_PROPERTY_NAME, artifactCoordinates.getVersion())
        .addParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME, ast);

    addCustomLoadingRequestParameters(loadingRequestBuilder);

    return of(loader.loadExtensionModel(loadingRequestBuilder.build()));
  }

  /**
   * @return The {@link ArtifactCoordinates} representing the artifact.
   * @throws ConfigurationException if the {@link ArtifactCoordinates} cannot be identified.
   */
  protected abstract ArtifactCoordinates getArtifactCoordinates() throws ConfigurationException;

  /**
   *
   * @return The {@link ExtensionModelLoader} to use for loading the artifact's {@link ExtensionModel}.
   * @throws ConfigurationException if the expected loader is not available.
   */
  protected abstract ExtensionModelLoader getLoader() throws ConfigurationException;

  /**
   * Allows for adding custom parameters to the loading request before the actual loading.
   * 
   * @param loadingRequestBuilder The loading request builder to add parameters to.
   */
  protected void addCustomLoadingRequestParameters(ExtensionModelLoadingRequest.Builder loadingRequestBuilder) {
    // Does nothing by default.
  }

  /**
   *
   * @param ast The artifact's {@link ArtifactAst}.
   * @return Whether the artifact contains reusable components.
   */
  protected abstract boolean containsReusableComponents(ArtifactAst ast);
}
