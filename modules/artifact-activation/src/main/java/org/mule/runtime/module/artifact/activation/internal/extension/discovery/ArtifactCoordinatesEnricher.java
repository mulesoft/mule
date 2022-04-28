/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.extension.discovery;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;

/**
 * Add the {@link ArtifactCoordinates} to the {@link ExtensionModel}.
 *
 * @since 4.5
 */
public class ArtifactCoordinatesEnricher implements DeclarationEnricher {

  private final ArtifactCoordinates artifactPluginDescriptor;

  public ArtifactCoordinatesEnricher(ArtifactCoordinates artifactPluginDescriptor) {
    this.artifactPluginDescriptor = artifactPluginDescriptor;
  }

  @Override public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    extensionLoadingContext.getExtensionDeclarer().withArtifactCoordinates(artifactPluginDescriptor);
  }
}
