/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.REDELIVERY_EXHAUSTED;

/**
 * Handler that will propagate errors and rollback transactions. Replaces the rollback-exception-strategy from Mule 3.
 *
 * @since 4.0
 */
public class OnErrorPropagateHandler extends TemplateOnErrorHandler {

  @Inject
  private ErrorTypeRepository errorTypeRepository;

  private SingleErrorTypeMatcher redeliveryExhaustedMatcher;

  @Override
  protected void doInitialise() throws InitialisationException {
    super.doInitialise();

    ErrorType redeliveryExhaustedErrorType = errorTypeRepository.getErrorType(REDELIVERY_EXHAUSTED)
        .orElseThrow(() -> new IllegalStateException("REDELIVERY_EXHAUSTED error type not found"));

    redeliveryExhaustedMatcher = new SingleErrorTypeMatcher(redeliveryExhaustedErrorType);
  }

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

  /**
   * {@inheritDoc}
   *
   * @param buildFor
   */
  @Override
  public TemplateOnErrorHandler duplicateFor(ComponentLocation buildFor) {
    OnErrorPropagateHandler cpy = new OnErrorPropagateHandler();
    cpy.setFlowLocation(buildFor);
    when.ifPresent(expr -> cpy.setWhen(expr));
    cpy.setHandleException(this.handleException);
    cpy.setErrorType(this.errorType);
    cpy.setMessageProcessors(this.getMessageProcessors());
    cpy.setEnableNotifications(this.isEnableNotifications());
    cpy.setLogException(this.logException);
    cpy.setNotificationFirer(this.notificationFirer);
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
      return error.map(e -> redeliveryExhaustedMatcher.match(e.getErrorType()))
          .orElse(false);
    }
    return false;
  }

}
