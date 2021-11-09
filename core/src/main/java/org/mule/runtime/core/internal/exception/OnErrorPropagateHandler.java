/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.REDELIVERY_EXHAUSTED;

/**
 * Handler that will propagate errors and rollback transactions. Replaces the rollback-exception-strategy from Mule 3.
 *
 * @since 4.0
 */
public class OnErrorPropagateHandler extends TemplateOnErrorHandler {

  @Override
  public boolean acceptsAll() {
    return errorTypeMatcher == null && !when.isPresent();
  }

  /**
   * @param errorType an {@link ErrorType}
   * @return whether this handler accepts the provided type
   */
  boolean acceptsErrorType(ErrorType errorType) {
    return acceptsAll() || (errorTypeMatcher != null && errorTypeMatcher.match(errorType));
  }

  @Override
  protected Function<CoreEvent, CoreEvent> beforeRouting() {
    return event -> {
      Exception exception = getException(event);
      event = super.beforeRouting().apply(event);
      if (!isRedeliveryExhausted(exception) && isOwnedTransaction()) {
        rollback(exception);
      }
      return event;
    };
  }

  public void rollback(Exception ex) {
    TransactionCoordination.getInstance().rollbackCurrentTransaction();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TemplateOnErrorHandler duplicateFor(Location buildFor) {
    OnErrorPropagateHandler cpy = new OnErrorPropagateHandler();
    cpy.setFlowLocation(buildFor);
    when.ifPresent(expr -> cpy.setWhen(expr));
    cpy.setHandleException(this.handleException);
    cpy.setErrorType(this.errorType);
    cpy.setMessageProcessors(this.getMessageProcessors());
    cpy.setExceptionListener(this.getExceptionListener());
    cpy.setAnnotations(this.getAnnotations());
    return cpy;
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return new ArrayList<>(super.getOwnedMessageProcessors());
  }

  private boolean isRedeliveryExhausted(Exception exception) {
    if (exception instanceof MessagingException) {
      Optional<Error> error = ((MessagingException) exception).getEvent().getError();
      if (error.isPresent()) {
        String errorIdentifier = error.get().getErrorType().getIdentifier();
        return errorIdentifier.equals(REDELIVERY_EXHAUSTED.getName());
      }
    }
    return false;
  }

}
