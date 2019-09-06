/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.POST_STRUCTURE;

import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithParametersDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.util.DeclarationWalker;
import org.mule.runtime.extension.api.annotation.metadata.RequiredForMetadata;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link DeclarationEnricher} implementation which introspect Configurations and Connection Provides and looks
 * for parameters declared as {@link RequiredForMetadata}. If at least one is detected a {@link RequiredForMetadataModelProperty}
 * will be added in the config or connection provider indicating which are the required parameters for metadata resolution.
 *
 * @since 4.2.0
 */
public class RequiredForMetadataDeclarationEnricher implements DeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return POST_STRUCTURE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new DeclarationWalker() {

      @Override
      protected void onConfiguration(ConfigurationDeclaration declaration) {
        registerRequiredParametersForMetadata(declaration);
      }

      @Override
      protected void onConnectionProvider(ConnectedDeclaration owner, ConnectionProviderDeclaration declaration) {
        registerRequiredParametersForMetadata(declaration);
      }
    }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  private <T extends BaseDeclaration & WithParametersDeclaration> void registerRequiredParametersForMetadata(T declaration) {
    List<String> parametersRequiredForMetadata = getParametersNameRequiredForMetadata(declaration);
    if (!parametersRequiredForMetadata.isEmpty()) {
      declaration.addModelProperty(new RequiredForMetadataModelProperty(parametersRequiredForMetadata));
    }
  }

  /**
   * Filters the parameters of the given declaration and retrieves the ones that are required for Metadata Resolution.
   *
   * In case the annotation {@link RequiredForMetadata} is not present in any parameter, then, all will be considered as
   * required for metadata (Except from the Infrastructure parameters, which are never required for metadata).
   */
  private List<String> getParametersNameRequiredForMetadata(WithParametersDeclaration declaration) {
    List<ParameterDeclaration> nonInfrastructureParameters = getNonInfrastructureParameterNames(declaration.getAllParameters());
    List<String> nonInfrastructureParameterNames = new ArrayList<>();
    List<String> parametersRequiredForMetadata =
        nonInfrastructureParameters
            .stream()
            .peek(pd -> nonInfrastructureParameterNames.add(pd.getName())) //Add now in case this list is empty
            .filter(p -> p.getModelProperty(ExtensionParameterDescriptorModelProperty.class)
                .map(
                     mp -> mp.getExtensionParameter().isAnnotatedWith(RequiredForMetadata.class))
                .orElse(false))
            .map(NamedDeclaration::getName)
            .collect(toList());
    if (parametersRequiredForMetadata.isEmpty()) {
      return nonInfrastructureParameterNames;
    }
    return parametersRequiredForMetadata;
  }

  private List<ParameterDeclaration> getNonInfrastructureParameterNames(List<ParameterDeclaration> parametersDeclaration) {
    return parametersDeclaration
        .stream()
        .filter(
                pd -> !pd.getModelProperty(InfrastructureParameterModelProperty.class).isPresent())
        .collect(toList());
  }
}
