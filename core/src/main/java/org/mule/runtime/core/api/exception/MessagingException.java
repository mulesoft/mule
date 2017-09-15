/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.exception;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.mule.runtime.core.internal.config.ExceptionHelper.traverseCauseHierarchy;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.internal.config.ExceptionHelper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * <code>MessagingException</code> is a general message exception thrown when errors specific to Message processing occur..
 */

public class MessagingException extends MuleException {

  public static final String PAYLOAD_INFO_KEY = "Payload";
  public static final String PAYLOAD_TYPE_INFO_KEY = "Payload Type";

  /**
   * Serial version
   */
  private static final long serialVersionUID = 6941498759267936649L;

  /**
   * The Message being processed when the error occurred
   */
  protected transient Message muleMessage;

  /**
   * The MuleEvent being processed when the error occurred
   */
  protected final transient BaseEvent event;

  protected transient BaseEvent processedEvent;

  private boolean causeRollback;
  private boolean handled;
  private boolean inErrorHandler;
  private transient Component failingComponent;

  /**
   * @deprecated use MessagingException(Message, MuleEvent)
   */
  @Deprecated
  public MessagingException(I18nMessage message, Message muleMessage, MuleContext context) {
    super(message);
    this.muleMessage = muleMessage;
    this.event = null;
    storeErrorTypeInfo();
    setMessage(generateMessage(message, context));
  }

  public MessagingException(I18nMessage message, BaseEvent event) {
    super(message);
    this.event = event;
    extractMuleMessage(event);
    storeErrorTypeInfo();
    setMessage(generateMessage(message, null));
  }

  public MessagingException(I18nMessage message, BaseEvent event, Component failingComponent) {
    super(message);
    this.event = event;
    extractMuleMessage(event);
    this.failingComponent = failingComponent;
    storeErrorTypeInfo();
    setMessage(generateMessage(message, null));
  }

  /**
   * @deprecated use MessagingException(Message, MuleEvent, Throwable)
   */
  @Deprecated
  public MessagingException(I18nMessage message, Message muleMessage, MuleContext context, Throwable cause) {
    super(message, getCause(cause));
    this.muleMessage = muleMessage;
    this.event = null;
    storeErrorTypeInfo();
    setMessage(generateMessage(message, context));
  }

  public MessagingException(I18nMessage message, BaseEvent event, Throwable cause) {
    super(message, getCause(cause));
    this.event = getEvent(event, cause);
    extractMuleMessage(event);
    storeErrorTypeInfo();
    setMessage(generateMessage(message, null));
  }

  public MessagingException(I18nMessage message, BaseEvent event, Throwable cause, Component failingComponent) {
    super(message, getCause(cause));
    this.event = getEvent(event, cause);
    extractMuleMessage(event);
    this.failingComponent = failingComponent;
    storeErrorTypeInfo();
    setMessage(generateMessage(message, null));
  }

  public MessagingException(BaseEvent event, Throwable cause) {
    super(getCause(cause));
    this.event = getEvent(event, cause);
    extractMuleMessage(event);
    storeErrorTypeInfo();
    setMessage(generateMessage(getI18nMessage(), null));
  }

  public MessagingException(BaseEvent event, MessagingException original) {
    super(original.getI18nMessage(), getCause(original.getCause()));
    this.event = getEvent(event, original.getCause());
    this.failingComponent = original.getFailingComponent();
    this.causeRollback = original.causedRollback();
    this.handled = original.handled();
    original.getInfo().forEach((key, value) -> addInfo(key, value));
    extractMuleMessage(event);
    storeErrorTypeInfo();
    setMessage(original.getMessage());
  }

  public MessagingException(BaseEvent event, Throwable cause, Component failingComponent) {
    super(getCause(cause));
    this.event = getEvent(event, cause);
    extractMuleMessage(event);
    this.failingComponent = failingComponent;
    storeErrorTypeInfo();
    setMessage(generateMessage(getI18nMessage(), null));
  }

  private BaseEvent getEvent(BaseEvent event, Throwable cause) {
    return cause instanceof TypedException ? eventWithError(event, cause) : event;
  }

  private BaseEvent eventWithError(BaseEvent event, Throwable cause) {
    return BaseEvent.builder(event)
        .error(ErrorBuilder.builder(cause.getCause()).errorType(((TypedException) cause).getErrorType()).build()).build();
  }

  private void storeErrorTypeInfo() {
    if (event != null) {
      addInfo(INFO_ERROR_TYPE_KEY, getEvent().getError().map(e -> e.getErrorType().toString()).orElse(MISSING_DEFAULT_VALUE));
    }
  }

  protected String generateMessage(I18nMessage message, MuleContext muleContext) {
    StringBuilder buf = new StringBuilder(80);

    if (message != null) {
      buf.append(message.getMessage());
      String trimmedMessage = message.getMessage().trim();
      if (StringUtils.isNotBlank(trimmedMessage) && trimmedMessage.charAt(trimmedMessage.length() - 1) != '.') {
        buf.append(".");
      }
    }

    if (muleMessage != null) {
      if (MuleException.isVerboseExceptions()) {
        Object payload = muleMessage.getPayload().getValue();

        if (muleMessage.getPayload().getDataType().isStreamType()) {
          addInfo(PAYLOAD_INFO_KEY, abbreviate(payload.toString(), 1000));
        } else {
          if (payload != null) {
            addInfo(PAYLOAD_TYPE_INFO_KEY, muleMessage.getPayload().getDataType().getType().getName());
            if (muleContext != null) {
              // TODO MULE-10266 review how the transformationService is obtained when building an exception.
              try {
                addInfo(PAYLOAD_INFO_KEY,
                        muleContext.getTransformationService().internalTransform(muleMessage, DataType.STRING).getPayload()
                            .getValue());
              } catch (Exception e) {
                addInfo(PAYLOAD_INFO_KEY, format("%s while getting payload: %s", e.getClass().getName(), e.getMessage()));
              }
              addInfo(PAYLOAD_INFO_KEY, muleMessage.toString());
            }
          } else {
            addInfo(PAYLOAD_TYPE_INFO_KEY, Objects.toString(null));
            addInfo(PAYLOAD_INFO_KEY, Objects.toString(null));
          }
        }
      }
    } else {
      buf.append("The current Message is null!");
      addInfo(PAYLOAD_INFO_KEY, Objects.toString(null));
    }

    return buf.toString();
  }

  /**
   * @deprecated use {@link #getEvent().getMessage()} instead
   */
  @Deprecated
  public Message getMuleMessage() {
    if ((getEvent() != null)) {
      return getEvent().getMessage();
    }
    return muleMessage;
  }

  /**
   * @return event associated with the exception
   */
  public BaseEvent getEvent() {
    return processedEvent != null ? processedEvent : event;
  }

  /**
   * Sets the event that should be processed once this exception is caught
   *
   * @param processedEvent event bounded to the exception
   */
  public void setProcessedEvent(BaseEvent processedEvent) {
    if (processedEvent != null) {
      this.processedEvent = processedEvent;
      extractMuleMessage(processedEvent);
    } else {
      this.processedEvent = null;
      this.muleMessage = null;
    }
  }

  /**
   * Evaluates if the exception was caused (instance of) by the provided exception type
   *
   * @param e exception type to check against
   * @return true if the cause exception is an instance of the provided exception type
   */
  public boolean causedBy(final Class e) {
    if (e == null) {
      throw new IllegalArgumentException("Class cannot be null");
    }
    return (ExceptionHelper.traverseCauseHierarchy(this, causeException -> {
      if (e.isAssignableFrom(causeException.getClass())) {
        return causeException;
      }
      return null;
    }) != null);
  }

  /**
   * Evaluates if the exception was caused by the type and only the type provided exception type i,e: if cause exception is
   * NullPointerException will only return true if provided exception type is NullPointerException
   *
   * @param e exception type to check against
   * @return true if the cause exception is exaclty the provided exception type
   */
  public boolean causedExactlyBy(final Class<? extends Throwable> e) {
    if (e == null) {
      throw new IllegalArgumentException("Class cannot be null");
    }
    return (traverseCauseHierarchy(this, causeException -> {
      if (causeException.getClass().equals(e)) {
        return causeException;
      }
      return null;
    }) != null);
  }

  /**
   * @return the exception thrown by the failing message processor
   */
  public Throwable getRootCause() {
    Throwable rootException = ExceptionHelper.getRootException(this);
    if (rootException == null) {
      rootException = this;
    }
    return rootException;
  }

  /**
   * Checks the cause exception type name matches the provided regex. Supports any java regex
   *
   * @param regex regular expression to match against the exception type name
   * @return true if the exception matches the regex, false otherwise
   */
  public boolean causeMatches(final String regex) {
    if (regex == null) {
      throw new IllegalArgumentException("regex cannot be null");
    }
    final Boolean matched = traverseCauseHierarchy(this, e -> e.getClass().getName().matches(regex));
    return matched != null && matched;
  }

  /**
   * Signals if the exception cause rollback of any current transaction if any or if the message source should rollback incoming
   * message
   *
   * @return true if exception cause rollback, false otherwise
   */
  public boolean causedRollback() {
    return causeRollback;
  }

  /**
   * Marks exception as rollback cause. Useful for message sources that can provide some rollback mechanism.
   *
   * @param causeRollback
   */
  public void setCauseRollback(boolean causeRollback) {
    this.causeRollback = causeRollback;
  }

  /**
   * Marks an exception as handled so it won't be re-throwed
   *
   * @param handled true if the exception must be mark as handled, false otherwise
   */
  public void setHandled(boolean handled) {
    this.handled = handled;
  }

  /**
   * Signals if exception has been handled or not
   *
   * @return true if exception has been handled, false otherwise
   */
  public boolean handled() {
    return handled;
  }

  /**
   * Marks the exception so that onTerminate is called on the source instead of onError if not handled
   *
   * @param inErrorHandler true if the exception has occurred in an error handler, false otherwise
   */
  public void setInErrorHandler(boolean inErrorHandler) {
    this.inErrorHandler = inErrorHandler;
  }

  /**
   * Signals if the exception occurred in an error handler
   *
   * @return true if happened inside error handler
   */
  public boolean inErrorHandler() {
    return inErrorHandler;
  }

  /**
   * @return the Mule component that causes the failure
   */
  public Component getFailingComponent() {
    return failingComponent;
  }

  protected void extractMuleMessage(BaseEvent event) {
    this.muleMessage = event == null ? null : event.getMessage();
  }

  private void writeObject(ObjectOutputStream out) throws Exception {
    out.defaultWriteObject();
    if (this.failingComponent instanceof Serializable) {
      out.writeBoolean(true);
      out.writeObject(this.failingComponent);
    } else {
      out.writeBoolean(false);
    }
  }

  protected static Throwable getCause(Throwable cause) {
    return cause instanceof TypedException ? cause.getCause() : cause;
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();

    boolean failingComponentWasSerialized = in.readBoolean();
    if (failingComponentWasSerialized) {
      this.failingComponent = (Component) in.readObject();
    }
  }

  @Override
  public String toString() {
    return super.toString() + "; ErrorType: "
        + getEvent().getError().map(e -> e.getErrorType().toString()).orElse(MISSING_DEFAULT_VALUE);
  }
}
