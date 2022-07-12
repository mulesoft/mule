/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.execution;

import static org.mule.runtime.api.exception.ExceptionHelper.getExceptionsAsList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.util.ExceptionUtils.isUnknownMuleError;
import static org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext.from;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.component.execution.ComponentExecutionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.operation.construct.Operation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

/**
 * {@link CompletableComponentExecutor} implemenetation that executes {@link Operation} instances.
 *
 * @since 4.5.0
 */
public class MuleOperationExecutor implements CompletableComponentExecutor<ComponentModel>, Initialisable {

  private static final Message NULL_MESSAGE = Message.builder().nullValue().build();

  private final ComponentModel operationModel;
  private Operation operation;

  @Inject
  private MuleContext muleContext;

  public MuleOperationExecutor(ComponentModel operationModel) {
    this.operationModel = operationModel;
  }

  @Override
  public void initialise() throws InitialisationException {
    operation = (Operation) new DefaultRegistry(muleContext).lookupByName(operationModel.getName())
        .orElseThrow(() -> new InitialisationException(createStaticMessage(
                                                                           format("Operation '%s' not found in registry",
                                                                                  operationModel.getName())),
                                                       this));
  }

  @Override
  public void execute(ExecutionContext<ComponentModel> executionContext, ExecutorCallback callback) {
    ExecutionContextAdapter<ComponentModel> ctx = (ExecutionContextAdapter<ComponentModel>) executionContext;
    final CoreEvent inputEvent = ctx.getEvent();

    CoreEvent executionEvent = builder(inputEvent.getContext())
        .parameters(buildOperationParameters(inputEvent, ctx))
        .message(NULL_MESSAGE)
        .build();

    operation.execute(executionEvent).whenComplete((resultEvent, exception) -> {
      if (exception != null) {
        callback.error(tryCreateTypedExceptionFrom(exception));
      } else {
        callback.complete(builder(inputEvent)
            .message(resultEvent.getMessage())
            .build());
      }
    });
  }

  private Map<String, ?> buildOperationParameters(CoreEvent inputEvent, ExecutionContextAdapter<ComponentModel> ctx) {
    return from(inputEvent)
        .getOperationExecutionParams(ctx.getComponent().getLocation(), inputEvent.getContext().getId())
        .getParameters();
  }

  private static Throwable tryCreateTypedExceptionFrom(Throwable exception) {
    return getErrorType(exception).map(errorType -> wrapInTyped(exception, errorType)).orElse(exception);
  }

  private static Optional<ErrorType> getErrorType(Throwable exception) {
    List<Throwable> exceptionsAsList = getExceptionsAsList(exception);
    for (Throwable e : exceptionsAsList) {
      if (e instanceof MuleException) {
        ErrorType errorType = ((MuleException) e).getExceptionInfo().getErrorType();
        if (!isUnknownMuleError(errorType)) {
          return of(errorType);
        }
      } else if (e instanceof ComponentExecutionException) {
        Optional<Error> optionalError = ((ComponentExecutionException) e).getEvent().getError();
        if (optionalError.isPresent()) {
          return optionalError.map(Error::getErrorType);
        }
      }
    }

    return empty();
  }

  private static Throwable wrapInTyped(Throwable throwable, ErrorType errorType) {
    if (throwable instanceof TypedException) {
      return throwable;
    }
    return new TypedException(throwable, errorType);
  }
}
