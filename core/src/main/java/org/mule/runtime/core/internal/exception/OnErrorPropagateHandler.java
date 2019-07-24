/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.privileged.exception.MessageRedeliveredException;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.mule.runtime.core.privileged.transaction.TransactionAdapter;
import org.reactivestreams.Publisher;

/**
 * Handler that will propagate errors and rollback transactions. Replaces the rollback-exception-strategy from Mule 3.
 *
 * @since 4.0
 */
public class OnErrorPropagateHandler extends TemplateOnErrorHandler {

  @Override
  public boolean acceptsAll() {
    return errorTypeMatcher == null && when == null;
  }

  @Override
  protected Function<CoreEvent, CoreEvent> beforeRouting(Exception exception) {
    return event -> {
      event = super.beforeRouting(exception).apply(event);
      if (!isRedeliveryExhausted(exception) && isOwnedTransaction()) {
        rollback(exception);
      }
      return event;
    };
  }

  private boolean isTransactionInGlobalErrorHandler(String transactionRootContainer) {
    return flowLocation.isPresent() && transactionRootContainer.equals(flowLocation.get().getGlobalName());
  }

  private boolean isOwnedTransaction() {
    TransactionAdapter transaction = (TransactionAdapter) TransactionCoordination.getInstance().getTransaction();
    if (transaction == null || !transaction.getComponentLocation().isPresent()) {
      return false;
    }
    String transactionContainerName = transaction.getComponentLocation().get().getRootContainerName();
    String transactionLocation = transaction.getComponentLocation().get().getLocation();
    if (this.getLocation() == null) {
      // We are in a Default Error Handler (it has no Location defined)
      if (flowLocation.isPresent()) {
        // We are in a default error handler for a TryScope, which must have been replicated to match the tx location
        // to rollback it
        return transactionLocation.equals(flowLocation.get().toString());
      } else {
        // We are in a default error handler of a Flow
        return transactionContainerName.equals(this.getRootContainerLocation().getGlobalName());
      }
    }
    if (isTransactionInGlobalErrorHandler((transactionContainerName))) {
      // We are in a GlobalErrorHandler that is defined for the container (Flow or TryScope) that created the tx
      return true;
    } else if (flowLocation.isPresent()) {
      // We are in a Global Error Handler, which is not the one that created the Tx
      return false;
    }

    // We are in a simple scenario where the error handler's location ends with "/error-handler/1".
    // We cannot use the RootContainerLocation, since in case of nested TryScopes (the outer one creating the tx)
    // the RootContainerLocation will be the same for both, and we don't want the inner TryScope's OnErrorPropagate
    // to rollback the tx.
    String ehLocation = this.getLocation().getLocation();
    ehLocation = ehLocation.substring(0, ehLocation.lastIndexOf('/'));
    ehLocation = ehLocation.substring(0, ehLocation.lastIndexOf('/'));
    return (transactionContainerName.equals(this.getRootContainerLocation().getGlobalName()) &&
        ehLocation.equals(transactionLocation));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TemplateOnErrorHandler duplicateFor(Location buildFor) {
    OnErrorPropagateHandler cpy = new OnErrorPropagateHandler();
    cpy.setFlowLocation(buildFor);
    cpy.setWhen(this.when);
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
    return (exception instanceof MessageRedeliveredException);
  }

  @Override
  protected Function<CoreEvent, Publisher<CoreEvent>> route(Exception exception) {
    if (isRedeliveryExhausted(exception)) {
      logger.info("Message redelivery exhausted. No redelivery exhausted actions configured. Message consumed.");
    } else {
      return super.route(exception);
    }
    return event -> just(event);
  }

}
