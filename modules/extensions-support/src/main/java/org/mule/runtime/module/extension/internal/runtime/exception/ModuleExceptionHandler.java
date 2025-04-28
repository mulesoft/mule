/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.exception.ExceptionHelper.suppressIfPresent;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.loader.utils.ExtensionNamespaceUtils.getExtensionsNamespace;
import static org.mule.runtime.module.extension.internal.error.SdkErrorTypeDefinitionAdapter.from;
import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.LoadingCache;

/**
 * Handler of {@link ModuleException ModuleExceptions}, which given a {@link Throwable} checks whether the exceptions is a
 * {@link ModuleException}, and in that case it's converted to a {@link TypedException}.
 *
 * @since 4.0
 */
public class ModuleExceptionHandler {

  private final Set<ErrorModel> allowedErrorTypes;
  private final String extensionNamespace;
  private final boolean suppressErrors;

  private final LoadingCache<ErrorTypeDefinition, Function<Throwable, ErrorType>> errorTypeCache;

  public ModuleExceptionHandler(ComponentModel componentModel, ExtensionModel extensionModel,
                                ErrorTypeRepository typeRepository, boolean suppressErrors) {
    allowedErrorTypes = componentModel.getErrorModels();
    extensionNamespace = getExtensionsNamespace(extensionModel);
    this.suppressErrors = suppressErrors;

    errorTypeCache = newBuilder().build(errorDefinition -> {
      final Optional<ErrorType> errorTypeLookedUp = typeRepository.lookupErrorType(builder()
          .namespace(extensionNamespace)
          .name(errorDefinition.getType())
          .build());

      if (errorTypeLookedUp.isPresent()) {
        final ErrorType errorType = errorTypeLookedUp.get();

        if (isAllowedError(errorType)) {
          return exception -> errorType;
        } else {
          return exception -> {
            throw new MuleRuntimeException(createStaticMessage("The component '%s' from the connector '%s' attempted to throw '%s', but"
                + " only %s errors are allowed.", componentModel.getName(), extensionModel.getName(),
                                                               extensionNamespace + ":" + errorDefinition, allowedErrorTypes),
                                           exception.getCause());
          };
        }
      } else {
        return exception -> {
          throw new MuleRuntimeException(createStaticMessage("The component '%s' from the connector '%s' attempted to throw '%s', but it was not registered "
              + "in the Error Repository", componentModel.getName(), extensionModel.getName(),
                                                             extensionNamespace + ":" + errorDefinition),
                                         getExceptionCause(exception));
        };
      }
    });
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
      return handleTypedException(throwable, ((ModuleException) throwable).getType());
    } else if (throwable instanceof org.mule.sdk.api.exception.ModuleException) {
      return handleTypedException(throwable, ((org.mule.sdk.api.exception.ModuleException) throwable).getType());
    }
    return throwable;
  }

  private Throwable handleTypedException(final Throwable exception,
                                         org.mule.runtime.extension.api.error.ErrorTypeDefinition<?> errorDefinition) {
    return handleTypedException(exception, from(errorDefinition));
  }

  private Throwable handleTypedException(final Throwable exception, ErrorTypeDefinition<?> errorDefinition) {
    return new TypedException(getExceptionCause(exception), errorTypeCache.get(errorDefinition).apply(exception));
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
    // For subclasses of ModuleException, or if the ModuleException contains an error message and its cause does not,
    // we use it as it already contains additional information
    if (throwable.getClass().equals(ModuleException.class) ||
        throwable.getClass().equals(org.mule.sdk.api.exception.ModuleException.class)) {
      return throwable.getCause() != null && (throwable.getCause().getMessage() != null || throwable.getMessage() == null)
          ? suppressMessagingException(throwable.getCause())
          : new MuleRuntimeException(createStaticMessage(throwable.getMessage()), throwable.getCause());
    } else {
      return suppressMessagingException(throwable);
    }
  }

  /**
   * Suppresses MessagingExceptions if the {@link MuleRuntimeFeature#SUPPRESS_ERRORS} feature is enabled.
   *
   * @param throwable Throwable where the suppression will be done.
   * @return Throwable with the result of the suppression.
   * @see org.mule.runtime.api.exception.ExceptionHelper#suppressIfPresent
   */
  private Throwable suppressMessagingException(Throwable throwable) {
    if (suppressErrors) {
      return suppressIfPresent(throwable, MessagingException.class);
    } else {
      return throwable;
    }
  }
}
