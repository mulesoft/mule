/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static java.lang.String.format;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.RETRY_EXHAUSTED_ERROR_IDENTIFIER;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getExtensionsErrorNamespace;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.core.api.exception.Errors;
import org.mule.runtime.core.api.exception.ExceptionMapper;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
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

  public static final String MULE = Errors.CORE_NAMESPACE_NAME;
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

    errorTypes.forEach(errorModel -> getErrorType(errorModel, extensionModel));

    ExtensionWalker extensionWalker = new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel model) {
        if (!errorTypes.isEmpty()) {
          ExceptionMapper.Builder builder = ExceptionMapper.builder();
          builder.addExceptionMapping(ConnectionException.class, getErrorType(connectivityErrorModel, extensionModel));
          builder.addExceptionMapping(RetryPolicyExhaustedException.class, getErrorType(retryExhaustedError, extensionModel));

          String elementName = syntaxResolver.resolve(model).getElementName();
          errorTypeLocator.addComponentExceptionMapper(createIdentifier(elementName, extensionNamespace),
                                                       builder.build());
        }
      }
    };
    extensionWalker.walk(extensionModel);
  }

  private ErrorType getErrorType(ErrorModel errorModel, ExtensionModel extensionModel) {
    ComponentIdentifier identifier = createIdentifier(errorModel.getType(), errorModel.getNamespace());
    Optional<ErrorType> optionalError = errorTypeRepository.getErrorType(identifier);
    return optionalError.orElseGet(() -> createErrorType(errorModel, identifier, extensionModel));
  }

  private ErrorType createErrorType(ErrorModel errorModel, ComponentIdentifier identifier, ExtensionModel extensionModel) {
    final ErrorType errorType;

    if (identifier.getNamespace().equals(MULE)) {
      throw new MuleRuntimeException(createStaticMessage(format("The extension [%s] tried to register the [%s] error with [%s] namespace, which is not allowed.",
                                                                extensionModel.getName(), identifier, MULE)));
    }

    if (errorModel.getParent().isPresent()) {
      errorType = errorTypeRepository.addErrorType(identifier, getErrorType(errorModel.getParent().get(), extensionModel));
    } else {
      errorType = errorTypeRepository.addErrorType(identifier, null);
    }
    return errorType;
  }

  private static ComponentIdentifier createIdentifier(String name, String namespace) {
    return builder().name(name).namespace(namespace).build();
  }
}
