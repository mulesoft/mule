/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.exception;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.meta.Typed;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.CoreEvent.Builder;
import org.mule.runtime.core.internal.message.ErrorBuilder;

/**
 * Representation of a failure during reactive processing of a {@link CoreEvent}, associates both failure and event taking into
 * account {@link TypedException} instances.
 *
 * @since 4.0
 */
public class EventProcessingException extends MuleException {

  private static final long serialVersionUID = 8849038142532938068L;

  protected final transient CoreEvent event;

  public EventProcessingException(I18nMessage message, CoreEvent event) {
    super(message);
    this.event = event;
    storeErrorTypeInfo(event);
  }

  public EventProcessingException(I18nMessage message, CoreEvent event, Throwable cause) {
    super(message, getCause(cause));
    this.event = getEvent(event, cause);
    storeErrorTypeInfo(cause);
  }

  public EventProcessingException(CoreEvent event, Throwable cause) {
    super(getCause(cause));
    this.event = getEvent(event, cause);
    storeErrorTypeInfo(cause);
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

  private static CoreEvent getEvent(CoreEvent event, Throwable cause) {
    CoreEvent result = event;
    if (cause instanceof TypedException) {
      result = eventWithError(event, (TypedException) cause);
    } else if (cause instanceof EventProcessingException) {
      result = eventWithError(event, (EventProcessingException) cause);
    }
    return result;
  }

  private static CoreEvent eventWithError(CoreEvent event, TypedException cause) {
    return CoreEvent.builder(event)
        .error(ErrorBuilder.builder(cause.getCause()).errorType(cause.getErrorType()).build()).build();
  }

  private static CoreEvent eventWithError(CoreEvent event, EventProcessingException cause) {
    Builder coreEventBuilder = CoreEvent.builder(event);
    cause.getEvent().getError().ifPresent(error -> coreEventBuilder.error(ErrorBuilder.builder(error).build()));
    return coreEventBuilder.build();
  }

  private void storeErrorTypeInfo(Throwable cause) {
    if (cause instanceof TypedException) {
      addInfo(INFO_ERROR_TYPE_KEY, ((TypedException) cause).getErrorType().toString());
    } else if (cause instanceof EventProcessingException) {
      addInfo(INFO_ERROR_TYPE_KEY,
              ((EventProcessingException) cause).getInfo().getOrDefault(INFO_ERROR_TYPE_KEY, MISSING_DEFAULT_VALUE));
    } else {
      storeErrorTypeInfo(event);
    }
  }

  private void storeErrorTypeInfo(CoreEvent event) {
    event.getError().ifPresent(e -> addInfo(INFO_ERROR_TYPE_KEY, e.getErrorType()));
  }

}
