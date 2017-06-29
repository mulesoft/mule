/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static org.mule.runtime.core.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.extension.api.error.MuleErrors.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.extension.api.error.MuleErrors.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.extension.api.error.MuleErrors.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.extension.api.error.MuleErrors.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.module.extension.internal.loader.enricher.ModuleErrors.CONNECTIVITY;
import static org.mule.runtime.module.extension.internal.loader.enricher.ModuleErrors.RETRY_EXHAUSTED;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectivityModelProperty;

import java.util.Set;

/**
 * {@link DeclarationEnricher} implementation which enriches {@link OperationModel operationModels} adding connectivity related
 * {@link MuleErrors} if the operations are considered as a connected ones.
 *
 * @since 4.0
 */
public class ExtensionsErrorsDeclarationEnricher implements DeclarationEnricher {

  private static final String MULE = CORE_NAMESPACE_NAME;

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new ExtensionsErrorsDeclarer(extensionLoadingContext).apply();
  }

  private static final class ExtensionsErrorsDeclarer {

    private final ExtensionLoadingContext extensionLoadingContext;
    private ErrorsModelFactory muleErrorsModelFactory;

    public ExtensionsErrorsDeclarer(ExtensionLoadingContext extensionLoadingContext) {
      this.extensionLoadingContext = extensionLoadingContext;
    }

    private void apply() {
      ExtensionDeclaration extensionDeclaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
      muleErrorsModelFactory = new ErrorsModelFactory(MuleErrors.class.getEnumConstants(), MULE);
      Set<ErrorModel> errorModels = extensionDeclaration.getErrorModels();
      new IdempotentDeclarationWalker() {

        @Override
        public void onOperation(WithOperationsDeclaration owner, OperationDeclaration operationDeclaration) {
          if (operationDeclaration.getModelProperty(ConnectivityModelProperty.class).isPresent()) {
            operationDeclaration.addErrorModel(getErrorModel(CONNECTIVITY, errorModels, operationDeclaration));
            operationDeclaration.addErrorModel(getErrorModel(RETRY_EXHAUSTED, errorModels, operationDeclaration));
          }
        }

        @Override
        protected void onSource(WithSourcesDeclaration owner, SourceDeclaration sourceDeclaration) {
          sourceDeclaration.getSuccessCallback().ifPresent(callback -> {
            registerError(SOURCE_RESPONSE_SEND, sourceDeclaration, extensionDeclaration);
            registerError(SOURCE_RESPONSE_GENERATE, sourceDeclaration, extensionDeclaration);
          });

          sourceDeclaration.getErrorCallback().ifPresent(callback -> {
            registerError(SOURCE_ERROR_RESPONSE_SEND, sourceDeclaration, extensionDeclaration);
            registerError(SOURCE_ERROR_RESPONSE_GENERATE, sourceDeclaration, extensionDeclaration);
          });
        }
      }.walk(extensionDeclaration);
    }

    private void registerError(MuleErrors error, SourceDeclaration sourceDeclaration, ExtensionDeclaration extensionDeclaration) {
      ErrorModel errorModel = muleErrorsModelFactory.getErrorModel(error);
      extensionDeclaration.addErrorModel(errorModel);
      sourceDeclaration.addErrorModel(errorModel);
    }

    private ErrorModel getErrorModel(ErrorTypeDefinition<?> errorTypeDefinition, Set<ErrorModel> errorModels,
                                     ComponentDeclaration component) {
      return errorModels
          .stream()
          .filter(error -> !error.getNamespace().equals(MULE) && error.getType().equals(errorTypeDefinition.getType()))
          .findFirst()
          .orElseThrow(() -> new IllegalModelDefinitionException(format("Trying to add the '%s' Error to the Component '%s' but the Extension doesn't declare it",
                                                                        errorTypeDefinition, component.getName())));
    }
  }
}
