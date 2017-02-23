/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.core.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.RETRY_EXHAUSTED_ERROR_IDENTIFIER;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getExtensionsErrorNamespace;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.core.exception.ErrorTypeLocator;
import org.mule.runtime.core.exception.ErrorTypeRepository;
import org.mule.runtime.core.exception.ExceptionMapper;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.dsl.syntax.resolver.SingleExtensionImportTypesStrategy;

import java.util.Optional;
import java.util.Set;

/**
 * Extension's {@link ErrorType} registrant.
 * <p>
 * For each {@link OperationModel} from an {@link ExtensionModel} consumes the declared {@link ErrorModel}s converts these to
 * {@link ErrorType}s using the {@link ErrorTypeRepository}, and finally register an {@link ExceptionMapper} for the operation.
 *
 * @see ErrorModel
 * @see ErrorType
 * @see ErrorTypeRepository
 * @see ErrorTypeLocator
 * @since 4.0
 */
class ExtensionErrorsRegistrant {

  public static final String MULE = org.mule.runtime.core.exception.Errors.CORE_NAMESPACE_NAME;
  private final ErrorTypeRepository errorTypeRepository;
  private final ErrorTypeLocator errorTypeLocator;

  ExtensionErrorsRegistrant(ErrorTypeRepository errorTypeRepository, ErrorTypeLocator errorTypeLocator) {
    this.errorTypeRepository = errorTypeRepository;
    this.errorTypeLocator = errorTypeLocator;
  }

  /**
   * Registers the found {@link ErrorModel} from each {@link OperationModel} into the {@link ErrorTypeRepository} and creates an
   * {@link ExceptionMapper} for each {@link OperationModel} that declares {@link ErrorModel}s.
   *
   * @param extensionModel from where get the {@link ErrorModel} from each {@link OperationModel}
   */
  void registerErrors(ExtensionModel extensionModel) {
    Set<ErrorModel> errorTypes = extensionModel.getErrorModels();
    String extensionNamespace = extensionModel.getXmlDslModel().getPrefix();
    String errorExtensionNamespace = getExtensionsErrorNamespace(extensionModel);
    DslSyntaxResolver syntaxResolver = DslSyntaxResolver.getDefault(extensionModel, new SingleExtensionImportTypesStrategy());

    ErrorModel connectivityErrorModel = newError(CONNECTIVITY_ERROR_IDENTIFIER, errorExtensionNamespace)
        .withParent(newError(CONNECTIVITY_ERROR_IDENTIFIER, MULE).build()).build();

    ErrorModel retryExhaustedError = newError(RETRY_EXHAUSTED_ERROR_IDENTIFIER, errorExtensionNamespace)
        .withParent(newError(RETRY_EXHAUSTED_ERROR_IDENTIFIER, MULE).build()).build();

    errorTypes.forEach(this::getErrorType);

    ExtensionWalker extensionWalker = new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel model) {
        if (!errorTypes.isEmpty()) {
          ExceptionMapper.Builder builder = ExceptionMapper.builder();
          builder.addExceptionMapping(ConnectionException.class, getErrorType(connectivityErrorModel));
          builder.addExceptionMapping(RetryPolicyExhaustedException.class, getErrorType(retryExhaustedError));

          String elementName = syntaxResolver.resolve(model).getElementName();
          errorTypeLocator.addComponentExceptionMapper(createIdentifier(elementName, extensionNamespace),
                                                       builder.build());
        }
      }
    };
    extensionWalker.walk(extensionModel);
  }

  private ErrorType getErrorType(ErrorModel errorModel) {
    ComponentIdentifier identifier = createIdentifier(errorModel.getType(), errorModel.getNamespace());
    Optional<ErrorType> optionalError = errorTypeRepository.lookupErrorType(identifier);
    return optionalError.orElseGet(() -> createErrorType(errorModel, identifier));
  }

  private ErrorType createErrorType(ErrorModel errorModel, ComponentIdentifier identifier) {
    final ErrorType errorType;
    if (errorModel.getParent().isPresent()) {
      errorType = errorTypeRepository.addErrorType(identifier, getErrorType(errorModel.getParent().get()));
    } else {
      errorType = errorTypeRepository.addErrorType(identifier, null);
    }
    return errorType;
  }

  private static ComponentIdentifier createIdentifier(String name, String namespace) {
    return builder().withName(name).withNamespace(namespace).build();
  }
}
