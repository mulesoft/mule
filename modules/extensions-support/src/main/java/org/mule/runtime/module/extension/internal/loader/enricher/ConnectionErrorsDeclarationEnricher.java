/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.extension.api.error.MuleErrors.SOURCE_RESPONSE;
import static org.mule.runtime.extension.api.error.MuleErrors.SOURCE_RESPONSE_PARAMETERS;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.module.extension.internal.loader.enricher.ModuleErrors.CONNECTIVITY;
import static org.mule.runtime.module.extension.internal.loader.enricher.ModuleErrors.RETRY_EXHAUSTED;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.error.ErrorModelBuilder;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;

import java.util.Optional;
import java.util.Set;

/**
 * {@link DeclarationEnricher} implementation which enriches {@link OperationModel operationModels} adding connectivity
 * related {@link MuleErrors} if the operations are considered as a connected ones.
 *
 * @since 4.0
 */
public class ConnectionErrorsDeclarationEnricher implements DeclarationEnricher {

  private static final String MULE_NAMESPACE = CORE_PREFIX.toUpperCase();

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    Optional<ImplementingTypeModelProperty> implementingType = declaration.getModelProperty(ImplementingTypeModelProperty.class);

    if (implementingType.isPresent()) {
      ExtensionElement extensionElement = new ExtensionTypeWrapper<>(implementingType.get().getType());
      extensionElement.getAnnotation(ErrorTypes.class).ifPresent(errorTypesAnnotation -> {

        Set<ErrorModel> errorModels = declaration.getErrorModels();
        if (!errorModels.isEmpty()) {
          new IdempotentDeclarationWalker() {

            @Override
            public void onOperation(WithOperationsDeclaration owner, OperationDeclaration operationDeclaration) {
              if (operationDeclaration.getModelProperty(ConnectivityModelProperty.class).isPresent()) {
                operationDeclaration.addError(getErrorModel(CONNECTIVITY, errorModels, operationDeclaration));
                operationDeclaration.addError(getErrorModel(RETRY_EXHAUSTED, errorModels, operationDeclaration));
              }
            }

            @Override
            protected void onSource(WithSourcesDeclaration owner, SourceDeclaration sourceDeclaration) {
              if (sourceDeclaration.getSuccessCallback().isPresent() || sourceDeclaration.getErrorCallback().isPresent()) {
                ErrorModelBuilder ANY_ERROR = newError("ANY", MULE_NAMESPACE);
                declaration
                    .addErrorModel(newError(SOURCE_RESPONSE.getType(), MULE_NAMESPACE).withParent(ANY_ERROR.build()).build());
                declaration.addErrorModel(newError(SOURCE_RESPONSE_PARAMETERS.getType(), MULE_NAMESPACE)
                    .withParent(ANY_ERROR.build()).build());
                Set<ErrorModel> enrichedErrorModels = declaration.getErrorModels();

                sourceDeclaration.addError(getMuleErrorModel(SOURCE_RESPONSE_PARAMETERS, enrichedErrorModels, sourceDeclaration));
                sourceDeclaration.addError(getMuleErrorModel(SOURCE_RESPONSE, enrichedErrorModels, sourceDeclaration));
              }
            }
          }.walk(declaration);
        }
      });
    }
  }

  private ErrorModel getErrorModel(ErrorTypeDefinition<?> errorTypeDefinition, Set<ErrorModel> errorModels,
                                   ComponentDeclaration component) {
    return errorModels
        .stream()
        .filter(error -> !error.getNamespace().equals(MULE_NAMESPACE) && error.getType().equals(errorTypeDefinition.getType()))
        .findFirst()
        .orElseThrow(() -> new IllegalModelDefinitionException(format("Trying to add the '%s' Error to the Component '%s' but the Extension doesn't declare it",
                                                                      errorTypeDefinition, component.getName())));
  }

  private ErrorModel getMuleErrorModel(ErrorTypeDefinition<?> errorTypeDefinition, Set<ErrorModel> errorModels,
                                       ComponentDeclaration component) {
    return errorModels
        .stream()
        .filter(error -> error.getNamespace().equals(MULE_NAMESPACE) && error.getType().equals(errorTypeDefinition.getType()))
        .findFirst()
        .orElseThrow(() -> new IllegalModelDefinitionException(format("Trying to add the '%s' Error to the Component '%s' but the Extension doesn't declare it",
                                                                      errorTypeDefinition, component.getName())));
  }
}
