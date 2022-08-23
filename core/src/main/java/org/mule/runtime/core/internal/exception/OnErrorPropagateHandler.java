/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_CONTINUE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_ROLLBACK;
import static org.mule.runtime.config.internal.error.MuleCoreErrorTypeRepository.MULE_CORE_ERROR_TYPE_REPOSITORY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.REDELIVERY_EXHAUSTED;
import static org.mule.runtime.core.api.transaction.TransactionUtils.profileTransactionAction;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.ON_ERROR_PROPAGATE_ELEMENT_IDENTIFIER;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.context.TransactionProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Handler that will propagate errors and rollback transactions. Replaces the rollback-exception-strategy from Mule 3.
 *
 * @since 4.0
 */
public class OnErrorPropagateHandler extends TemplateOnErrorHandler {

  private static final String COMPONENT_IDENTIFIER = CORE_PREFIX + ":" + ON_ERROR_PROPAGATE_ELEMENT_IDENTIFIER;
  private final SingleErrorTypeMatcher redeliveryExhaustedMatcher;

  @Inject
  private ProfilingService profilingService;

  private ProfilingDataProducer<TransactionProfilingEventContext, Object> continueProducer;
  private ProfilingDataProducer<TransactionProfilingEventContext, Object> rollbackProducer;

  public OnErrorPropagateHandler() {
    ErrorType redeliveryExhaustedErrorType = MULE_CORE_ERROR_TYPE_REPOSITORY.getErrorType(REDELIVERY_EXHAUSTED)
        .orElseThrow(() -> new IllegalStateException("REDELIVERY_EXHAUSTED error type not found"));

    // Identifier for cases where this class is directly instantiated by the runtime core in order to implement default error
    // handling.
    setAnnotations(Collections.singletonMap(ANNOTATION_NAME, buildFromStringRepresentation(COMPONENT_IDENTIFIER)));

    redeliveryExhaustedMatcher = new SingleErrorTypeMatcher(redeliveryExhaustedErrorType);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    super.doInitialise();
    this.continueProducer = profilingService.getProfilingDataProducer(TX_CONTINUE);
    this.rollbackProducer = profilingService.getProfilingDataProducer(TX_ROLLBACK);
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
      if (!isRedeliveryExhausted(exception) && isOwnedTransaction(exception)) {
        profileTransactionAction(rollbackProducer, TX_ROLLBACK, getLocation());
        rollback(exception);
      } else {
        profileTransactionAction(continueProducer, TX_CONTINUE, getLocation());
      }
      return event;
    };
  }

  public void rollback(Exception ex) {
    TransactionCoordination.getInstance().rollbackCurrentTransaction();
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
      return error.map(e -> redeliveryExhaustedMatcher.match(e.getErrorType()))
          .orElse(false);
    }
    return false;
  }

}
