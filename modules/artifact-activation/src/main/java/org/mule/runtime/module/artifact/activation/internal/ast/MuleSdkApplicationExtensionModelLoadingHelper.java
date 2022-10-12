/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.ast;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION_DEF;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.boot.ExtensionLoaderUtils.getOptionalLoaderById;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_NAME_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_APPLICATION_LOADER_ID;

import static java.util.Collections.singleton;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;

import java.util.Optional;
import java.util.Set;

/**
 * An {@link MuleSdkExtensionModelLoadingHelper} suitable for the context of applications.
 *
 * @since 4.5.0
 */
public class MuleSdkApplicationExtensionModelLoadingHelper extends AbstractMuleSdkExtensionModelLoadingHelper {

  // TODO W-11796759: This class shouldn't know which are the specific reusable components.
  private static final Set<ComponentType> REUSABLE_COMPONENT_TYPES = singleton(OPERATION_DEF);

  private final String artifactId;
  private final Optional<ArtifactCoordinates> artifactCoordinates;
  private final ExpressionLanguageMetadataService expressionLanguageMetadataService;

  public MuleSdkApplicationExtensionModelLoadingHelper(String artifactId, Optional<ArtifactCoordinates> artifactCoordinates,
                                                       ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    this.artifactId = artifactId;
    this.artifactCoordinates = artifactCoordinates;
    this.expressionLanguageMetadataService = expressionLanguageMetadataService;
  }

  @Override
  protected ArtifactCoordinates getArtifactCoordinates() throws ConfigurationException {
    if (!artifactCoordinates.isPresent()) {
      throw new ConfigurationException(buildErrorMessage("No artifact coordinates specified", artifactId));
    }
    return artifactCoordinates.get();
  }

  @Override
  protected ExtensionModelLoader getLoader() throws ConfigurationException {
    return getOptionalLoaderById(this.getClass().getClassLoader(), MULE_SDK_APPLICATION_LOADER_ID)
        .orElseThrow(() -> new ConfigurationException(buildErrorMessage("Mule ExtensionModelLoader not found", artifactId)));
  }

  @Override
  protected boolean containsReusableComponents(ArtifactAst ast) {
    return ast.topLevelComponentsStream()
        .anyMatch(component -> REUSABLE_COMPONENT_TYPES.contains(component.getComponentType()));
  }

  @Override
  protected void addCustomLoadingRequestParameters(ExtensionModelLoadingRequest.Builder loadingRequestBuilder) {
    super.addCustomLoadingRequestParameters(loadingRequestBuilder);
    loadingRequestBuilder.addParameter(MULE_SDK_EXTENSION_NAME_PROPERTY_NAME, artifactId);
  }

  private I18nMessage buildErrorMessage(String reason, String artifactId) {
    return createStaticMessage("ExtensionModel for application %s not generated: %s", artifactId, reason);
  }
}
