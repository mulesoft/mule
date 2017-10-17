/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getExtensionsErrorNamespace;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.util.Set;

/**
 * Handler of {@link ModuleException ModuleExceptions}, which given a {@link Throwable} checks whether the exceptions is
 * {@link ModuleException}, and if is converts it to an {@link TypedException}.
 *
 * @since 4.0
 */
public class ModuleExceptionHandler {

  private final ComponentModel componentModel;
  private ExtensionModel extensionModel;
  private final ErrorTypeRepository typeRepository;
  private final Set<ErrorModel> allowedErrorTypes;
  private final String extensionNamespace;

  public ModuleExceptionHandler(ComponentModel componentModel, ExtensionModel extensionModel,
                                ErrorTypeRepository typeRepository) {

    this.componentModel = componentModel;
    this.extensionModel = extensionModel;
    this.typeRepository = typeRepository;

    allowedErrorTypes = componentModel.getErrorModels();
    extensionNamespace = getExtensionsErrorNamespace(extensionModel);
  }

  /**
   * Process a given {@link Throwable}, if this one is a {@link ModuleException}, a {@link TypedException} will be built and
   * returned.
   * <ul>
   * <li>The cause of the {@link TypedException} will be taken from the given {@code throwable} cause</li>
   * <li>The {@link ErrorType} will be taken from the {@link ErrorTypeRepository} using the {@link ErrorTypeDefinition} from the
   * {@link ModuleException}</li>
   * </ul>
   *
   * @param throwable to process
   */
  public Throwable processException(Throwable throwable) {
    if (throwable instanceof ModuleException) {
      ErrorTypeDefinition errorDefinition = ((ModuleException) throwable).getType();
      return handleTypedException(throwable, errorDefinition);
    }
    return throwable;
  }

  private Throwable handleTypedException(final Throwable exception, ErrorTypeDefinition errorDefinition) {
    ErrorType errorType = typeRepository.lookupErrorType(builder()
        .namespace(extensionNamespace)
        .name(errorDefinition.getType())
        .build())
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("The component '%s' from the connector '%s' attempted to throw '%s', but it was not registered "
            + "in the Error Repository", componentModel.getName(), extensionModel.getName(),
                                                                        extensionNamespace + ":" + errorDefinition),
                                                    getExceptionCause(exception)));

    if (!isAllowedError(errorType)) {
      throw new MuleRuntimeException(createStaticMessage("The component '%s' from the connector '%s' attempted to throw '%s', but"
          + " only %s errors are allowed.", componentModel.getName(), extensionModel.getName(),
                                                         extensionNamespace + ":" + errorDefinition, allowedErrorTypes),
                                     exception.getCause());
    }

    return new TypedException(getExceptionCause(exception), errorType);
  }


  private boolean isAllowedError(ErrorType errorType) {
    return allowedErrorTypes
        .stream()
        .anyMatch(errorModel -> {
          boolean isAllowed = false;
          ErrorType currentError = errorType;
          while (currentError != null && !isAllowed) {
            isAllowed = errorModel.getType().equals(currentError.getIdentifier())
                && errorModel.getNamespace().equals(currentError.getNamespace());
            currentError = currentError.getParentErrorType();
          }
          return isAllowed;
        });
  }

  private Throwable getExceptionCause(Throwable throwable) {
    return throwable.getClass().equals(ModuleException.class) ? throwable.getCause() : throwable;
  }
}
