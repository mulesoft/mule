/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.exception;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * Representation of a failure during reactive processing of a {@link CoreEvent}, associates both failure and event taking into
 * account {@link TypedException} instances.
 *
 * @since 4.0
 */
public class EventProcessingException extends MuleException {

  private static final long serialVersionUID = 8849038142532938070L;

  protected final transient CoreEvent event;

  public EventProcessingException(I18nMessage message, CoreEvent event) {
    super(message);
    this.event = event;
    storeErrorTypeInfo(event);
  }

  public EventProcessingException(I18nMessage message, CoreEvent event, Throwable cause) {
    super(message, getCause(cause));
    this.event = event;
    storeExceptionInfo(cause);
  }

  public EventProcessingException(CoreEvent event, Throwable cause) {
    this(event, cause, true);
  }

  public EventProcessingException(CoreEvent event, Throwable cause, boolean resolveType) {
    super(resolveType ? getCause(cause) : cause);
    this.event = event;
    storeExceptionInfo(cause);
  }

  public CoreEvent getEvent() {
    return event;
  }

  /**
   * @return the Mule component that causes the failure if known, {@code null} otherwise.
   */
  public Component getFailingComponent() {
    return null;
  }

  private static Throwable getCause(Throwable cause) {
    return cause instanceof TypedException ? cause.getCause() : cause;
  }

  private void storeExceptionInfo(Throwable cause) {
    storeErrorTypeInfo(cause);
    storeSuppressedCausesInfo(cause);
  }

  private void storeErrorTypeInfo(Throwable cause) {
    if (cause instanceof TypedException) {
      getExceptionInfo().setErrorType(((TypedException) cause).getErrorType());
    } else if (cause instanceof EventProcessingException) {
      getExceptionInfo().setErrorType(((EventProcessingException) cause).getExceptionInfo().getErrorType());
    } else {
      storeErrorTypeInfo(event);
    }
  }

  private void storeErrorTypeInfo(CoreEvent event) {
    event.getError().ifPresent(e -> getExceptionInfo().setErrorType(e.getErrorType()));
  }

  private void storeSuppressedCausesInfo(Throwable cause) {
    if (cause instanceof MuleException) {
      this.getExceptionInfo().setSuppressedCauses(((MuleException) cause).getExceptionInfo().getSuppressedCauses());
    }
  }

}
