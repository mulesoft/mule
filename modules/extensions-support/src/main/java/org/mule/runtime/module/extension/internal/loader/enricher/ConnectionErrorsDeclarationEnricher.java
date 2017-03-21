/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.module.extension.internal.loader.enricher.ModuleErrors.CONNECTIVITY;
import static org.mule.runtime.module.extension.internal.loader.enricher.ModuleErrors.RETRY_EXHAUSTED;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.error.ErrorModel;
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
            public void onOperation(WithOperationsDeclaration owner, OperationDeclaration operation) {
              if (operation.getModelProperty(ConnectivityModelProperty.class).isPresent()) {
                operation.addError(getErrorModel(CONNECTIVITY, errorModels, operation));
                operation.addError(getErrorModel(RETRY_EXHAUSTED, errorModels, operation));
              }
            }
          }.walk(declaration);
        }
      });
    }
  }

  private ErrorModel getErrorModel(ErrorTypeDefinition<?> errorTypeDefinition, Set<ErrorModel> errorModels,
                                   OperationDeclaration operation) {
    return errorModels
        .stream()
        .filter(error -> !error.getNamespace().equals(MULE_NAMESPACE) && error.getType().equals(errorTypeDefinition.getType()))
        .findFirst()
        .orElseThrow(() -> new IllegalModelDefinitionException(format("Trying to add the '%s' Error to the Component '%s' but the Extension doesn't declare it",
                                                                      errorTypeDefinition, operation.getName())));
  }
}
