/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.core.api.error.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.error.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.RETRY_EXHAUSTED_ERROR_IDENTIFIER;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getExtensionsNamespace;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.exception.ExceptionMapper;
import org.mule.runtime.core.api.exception.ExceptionMapper.Builder;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.dsl.syntax.resolver.SingleExtensionImportTypesStrategy;

import java.util.Set;

/**
 * Extension's {@link ErrorType} registrant.
 * <p>
 * For each {@link OperationModel} from an {@link ExtensionModel} consumes the declared {@link ErrorModel}s and registers an
 * {@link ExceptionMapper} for the operation.
 *
 * @see ErrorModel
 * @see ErrorType
 * @see ErrorTypeRepository
 * @see ErrorTypeLocator
 * @since 4.0
 */
public final class ExtensionErrorsRegistrant {

  public static void registerErrorMappings(final ErrorTypeRepository errorTypeRepository, final ErrorTypeLocator errorTypeLocator,
                                           final Set<ExtensionModel> dependencies) {
    dependencies
        .stream()
        .forEach(extModel -> {
          DslSyntaxResolver syntaxResolver = DslSyntaxResolver.getDefault(extModel, new SingleExtensionImportTypesStrategy());
          String nsp = getExtensionsNamespace(extModel);

          final Builder mapperBuilder = ExceptionMapper.builder();

          errorTypeRepository.lookupErrorType(builder().namespace(nsp).name(CONNECTIVITY_ERROR_IDENTIFIER).build())
              .filter(err -> err.getParentErrorType().getNamespace().equals(CORE_NAMESPACE_NAME)
                  && err.getParentErrorType().getIdentifier().equals(CONNECTIVITY_ERROR_IDENTIFIER))
              .ifPresent(err -> mapperBuilder.addExceptionMapping(ConnectionException.class, err));

          errorTypeRepository.lookupErrorType(builder().namespace(nsp).name(RETRY_EXHAUSTED_ERROR_IDENTIFIER).build())
              .filter(err -> err.getParentErrorType().getNamespace().equals(CORE_NAMESPACE_NAME)
                  && err.getParentErrorType().getIdentifier().equals(RETRY_EXHAUSTED_ERROR_IDENTIFIER))
              .ifPresent(err -> mapperBuilder.addExceptionMapping(RetryPolicyExhaustedException.class, err));

          addComponentExceptionMappers(errorTypeLocator, extModel, syntaxResolver, mapperBuilder.build());
        });
  }

  private static void addComponentExceptionMappers(final ErrorTypeLocator errorTypeLocator, ExtensionModel extModel,
                                                   DslSyntaxResolver syntaxResolver, final ExceptionMapper mapping) {
    extModel.getOperationModels()
        .stream()
        .map(model -> identifierFromModel(syntaxResolver, model))
        .forEach(identifier -> errorTypeLocator.addComponentExceptionMapper(identifier, mapping));
    extModel.getSourceModels()
        .stream()
        .map(model -> identifierFromModel(syntaxResolver, model))
        .forEach(identifier -> errorTypeLocator.addComponentExceptionMapper(identifier, mapping));
  }

  private static ComponentIdentifier identifierFromModel(DslSyntaxResolver syntaxResolver, ConnectableComponentModel model) {
    final DslElementSyntax dsl = syntaxResolver.resolve(model);
    return ComponentIdentifier.builder()
        .name(dsl.getElementName())
        .namespace(dsl.getPrefix())
        .build();
  }
}
