/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import static org.mule.runtime.api.exception.ExceptionHelper.getExceptionsAsList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.component.ComponentAnnotations.ANNOTATION_NAME;
import static org.mule.runtime.core.exception.ErrorMapping.ANNOTATION_ERROR_MAPPINGS;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.UNKNOWN;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.ErrorMessageAwareException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.ErrorMapping;
import org.mule.runtime.core.exception.ErrorTypeLocator;
import org.mule.runtime.core.exception.ErrorTypeRepository;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.exception.TypedException;
import org.mule.runtime.core.exception.WrapperErrorMessageAwareException;
import org.mule.runtime.core.message.ErrorBuilder;
import org.mule.runtime.core.util.ExceptionHandler;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;

/**
 * Mule exception utilities.
 */
public class ExceptionUtils extends org.apache.commons.lang.exception.ExceptionUtils {

  /**
   * This method returns true if the throwable contains a {@link Throwable} that matches the specified class or subclass in the
   * exception chain. Subclasses of the specified class do match.
   *
   * @param throwable the throwable to inspect, may be null
   * @param type the type to search for, subclasses match, null returns false
   * @return the index into the throwable chain, false if no match or null input
   */
  public static boolean containsType(Throwable throwable, Class<?> type) {
    return indexOfType(throwable, type) > -1;
  }

  /**
   * This method returns the throwable closest to the root cause that matches the specified class or subclass. Any null argument
   * will make the method return null.
   *
   * @param throwable the throwable to inspect, may be null
   * @param type the type to search for, subclasses match, null returns null
   * @return the throwable that is closest to the root in the throwable chain that matches the type or subclass of that type.
   */
  @SuppressWarnings("unchecked")
  public static <ET> ET getDeepestOccurrenceOfType(Throwable throwable, Class<ET> type) {
    if (throwable == null || type == null) {
      return null;
    }
    List<Throwable> throwableList = getThrowableList(throwable);
    ListIterator<Throwable> listIterator = throwableList.listIterator(throwableList.size());
    while (listIterator.hasPrevious()) {
      Throwable candidate = listIterator.previous();
      if (type.isAssignableFrom(candidate.getClass())) {
        return (ET) candidate;
      }
    }
    return null;
  }

  /**
   * Similar to {@link #getFullStackTrace(Throwable)} but removing the exception and causes messages. This is useful to determine
   * if two exceptions have matching stack traces regardless of the messages which may contain invokation specific data
   *
   * @param throwable the throwable to inspect, may be <code>null</code>
   * @return the stack trace as a string, with the messages stripped out. Empty string if throwable was <code>null</code>
   */
  public static String getFullStackTraceWithoutMessages(Throwable throwable) {
    StringBuilder builder = new StringBuilder();

    for (String frame : getStackFrames(throwable)) {
      builder.append(frame.replaceAll(":\\s+([\\w\\s]*.*)", "").trim()).append(LINE_SEPARATOR);
    }

    return builder.toString();
  }

  /**
   * Introspects the {@link Throwable} parameter to obtain the first {@link Throwable} of type {@link ConnectionException} in the
   * exception chain.
   *
   * @param throwable the last throwable in the exception chain.
   * @return an {@link Optional} value with the first {@link ConnectionException} in the exception chain if any.
   */
  public static Optional<ConnectionException> extractConnectionException(Throwable throwable) {
    return extractOfType(throwable, ConnectionException.class);
  }

  /**
   * Introspects the {@link Throwable} parameter to obtain the first {@link Throwable} of type {@code throwableType} in the
   * exception chain and return the cause of it.
   *
   * @param throwable the last throwable on the exception chain.
   * @param throwableType the type of the throwable that the cause is wanted.
   * @return the cause of the first {@link Throwable} of type {@code throwableType}.
   */
  public static Optional<Throwable> extractCauseOfType(Throwable throwable, Class<? extends Throwable> throwableType) {
    Optional<? extends Throwable> typeThrowable = extractOfType(throwable, throwableType);
    return typeThrowable.isPresent() ? ofNullable(typeThrowable.get().getCause()) : empty();
  }

  /**
   * Introspects the {@link Throwable} parameter to obtain the first {@link Throwable} of type {@code throwableType} in the
   * exception chain.
   * <p>
   * This method handles recursive cause structures that might otherwise cause infinite loops. If the throwable parameter is a
   * {@link ConnectionException} the same value will be returned. If the throwable parameter has a cause of itself, then an empty
   * value will be returned.
   *
   * @param throwable the last throwable on the exception chain.
   * @param throwableType the type of the throwable is wanted to find.
   * @return the cause of the first {@link Throwable} of type {@code throwableType}.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Throwable> Optional<T> extractOfType(Throwable throwable, Class<T> throwableType) {
    if (throwable == null || !containsType(throwable, throwableType)) {
      return empty();
    }

    return (Optional<T>) stream(getThrowables(throwable)).filter(throwableType::isInstance).findFirst();
  }

  /**
   * Executes the given {@code callable} knowing that it might throw an {@link Exception} of type {@code expectedExceptionType}.
   * If that happens, then it will re throw such exception.
   * <p>
   * If the {@code callable} throws a {@link RuntimeException} of a different type, then it is also re-thrown. Finally, if an
   * exception of any different type is thrown, then it is handled by delegating into the {@code exceptionHandler}, which might in
   * turn also throw an exception or handle it returning a value.
   *
   * @param expectedExceptionType the type of exception which is expected to be thrown
   * @param callable the delegate to be executed
   * @param exceptionHandler a {@link ExceptionHandler} in case an unexpected exception is found instead
   * @param <T> the generic type of the return value
   * @param <E> the generic type of the expected exception
   * @return a value returned by either the {@code callable} or the {@code exceptionHandler}
   * @throws E if the expected exception is actually thrown
   */
  public static <T, E extends Exception> T tryExpecting(Class<E> expectedExceptionType, Callable<T> callable,
                                                        ExceptionHandler<T, E> exceptionHandler)
      throws E {
    try {
      return callable.call();
    } catch (Exception e) {
      if (expectedExceptionType.isInstance(e)) {
        throw (E) e;
      }

      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }

      return exceptionHandler.handle(e);
    }
  }

  /**
   * Determine the {@link ErrorType} of a given exception thrown by a given message processor.
   *
   * @param annotatedObject the component that threw the exception.
   * @param exception the exception thrown.
   * @param errorTypeLocator the {@link ErrorTypeLocator}.
   * @return the resolved {@link ErrorType}
   */
  public static ErrorType getErrorTypeFromFailingProcessor(Object annotatedObject, Throwable exception,
                                                           ErrorTypeLocator errorTypeLocator) {
    ErrorType errorType;
    Throwable causeException =
        exception instanceof WrapperErrorMessageAwareException ? ((WrapperErrorMessageAwareException) exception).getRootCause()
            : exception;
    ComponentIdentifier componentIdentifier = null;
    List<ErrorMapping> errorMappings = null;
    if (AnnotatedObject.class.isAssignableFrom(annotatedObject.getClass())) {
      componentIdentifier =
          (ComponentIdentifier) ((AnnotatedObject) annotatedObject).getAnnotation(ANNOTATION_NAME);
      errorMappings = (List<ErrorMapping>) ((AnnotatedObject) annotatedObject).getAnnotation(ANNOTATION_ERROR_MAPPINGS);
    }
    if (causeException instanceof TypedException) {
      errorType = ((TypedException) causeException).getErrorType();
    } else if (componentIdentifier != null) {
      errorType = errorTypeLocator.lookupComponentErrorType(componentIdentifier, causeException);
    } else {
      errorType = errorTypeLocator.lookupErrorType(causeException);
    }

    if (errorMappings != null && !errorMappings.isEmpty()) {
      Optional<ErrorMapping> matchedErrorMapping = errorMappings.stream().filter(mapping -> mapping.match(errorType)).findFirst();
      if (matchedErrorMapping.isPresent()) {
        return matchedErrorMapping.get().getTarget();
      }
    }
    return errorType;
  }

  public static MessagingException putContext(MessagingException messagingException, Processor failingMessageProcessor,
                                              Event event, FlowConstruct flowConstruct, MuleContext muleContext) {
    EnrichedNotificationInfo notificationInfo =
        createInfo(event, messagingException, null);
    for (ExceptionContextProvider exceptionContextProvider : muleContext.getExceptionContextProviders()) {
      for (Map.Entry<String, Object> contextInfoEntry : exceptionContextProvider
          .getContextInfo(notificationInfo, failingMessageProcessor, flowConstruct).entrySet()) {
        if (!messagingException.getInfo().containsKey(contextInfoEntry.getKey())) {
          messagingException.getInfo().put(contextInfoEntry.getKey(), contextInfoEntry.getValue());
        }
      }
    }
    return messagingException;
  }

  /**
   * Create new {@link Event} with {@link org.mule.runtime.api.message.Error} instance set.
   *
   * @param currentEvent event when error occured.
   * @param annotatedObject message processor/source.
   * @param messagingException messaging exception.
   * @param errorTypeLocator the mule context.
   * @return new {@link Event} with relevant {@link org.mule.runtime.api.message.Error} set.
   */
  public static Event createErrorEvent(Event currentEvent, Object annotatedObject, MessagingException messagingException,
                                       ErrorTypeLocator errorTypeLocator) {
    // TODO: MULE-10970/MULE-10971 - Change signature to AnnotatedObject once every processor and source is one
    Throwable causeException = messagingException.getCause() != null ? messagingException.getCause() : messagingException;
    Optional<Error> error = messagingException.getEvent().getError();
    if (!error.isPresent() || errorCauseMatchesException(causeException, error)
        || !messagingException.causedExactlyBy(error.get().getCause().getClass())) {

      Error newError = getErrorFromFailingProcessor(annotatedObject, causeException, errorTypeLocator);
      Event event = Event.builder(messagingException.getEvent()).error(newError).build();
      messagingException.setProcessedEvent(event);
      return event;
    } else {
      return currentEvent;
    }
  }

  /**
   * Updates the {@link MessagingException} to be thrown based on the content of the {@code exception} parameter and the chain of
   * causes inside it.
   * 
   * @param logger instance to use for logging
   * @param processor the failing processor
   * @param exception the exception to update based on it's content
   * @param errorTypeLocator the error type locator
   * @param errorTypeRepository the error type repository
   * @param flowConstruct the flow associated with the exception
   * @param muleContext the context of the artifact
   * @return a {@link MessagingException} with the proper {@link Error} associated to it's {@link Event}
   */
  public static MessagingException updateMessagingException(Logger logger, Processor processor, MessagingException exception,
                                                            ErrorTypeLocator errorTypeLocator,
                                                            ErrorTypeRepository errorTypeRepository, FlowConstruct flowConstruct,
                                                            MuleContext muleContext) {
    Optional<Exception> rootExceptionOptional =
        findRootExceptionForErrorHandling(exception, processor, errorTypeLocator, errorTypeRepository);

    Exception rootException;
    if (rootExceptionOptional.isPresent()) {
      if (logger.isDebugEnabled()) {
        logger.debug("discarding exception that is wrapping the original error", exception);
      }
      if (rootExceptionOptional.get() instanceof MessagingException
          && ((MessagingException) rootExceptionOptional.get()).getEvent().getError().isPresent()) {
        return (MessagingException) rootExceptionOptional.get();
      }
      rootException = rootExceptionOptional.get();
    } else {
      rootException = exception;
    }

    Processor failing = exception.getFailingMessageProcessor();
    if (failing == null && rootException instanceof MessagingException) {
      failing = ((MessagingException) rootException).getFailingMessageProcessor();
    }

    if (failing == null) {
      failing = processor;
      exception = new MessagingException(createStaticMessage(rootException.getMessage()), exception.getEvent(),
                                         rootException instanceof MessagingException ? rootException.getCause() : rootException,
                                         processor);
    }
    exception
        .setProcessedEvent(createErrorEvent(exception.getEvent(), processor, exception, errorTypeLocator));
    return putContext(exception, failing, exception.getEvent(), flowConstruct, muleContext);
  }

  /**
   * Searches for the root {@link Exception} to use to generate the {@link Error} inside the {@link Event}.
   * <p>
   * If such exception exists, then it's because the exception is wrapping an exception that already has an error. For instance, a
   * streaming error. Or it may also be that there's a wrapper but just for throwing a more specific for details exception.
   * <p>
   * If there's already a {@link MessagingException} with an {@link Event} that contains a non empty {@link Error} then that
   * exception will be returned since it means that the whole process of creating the error was already executed.
   *
   * @param exception the exception to search in all it's causes for a {@link MessagingException} with an {@link Error}
   * @param processor the processor that thrown the exception
   * @param errorTypeLocator the locator to discover {@link ErrorType}s
   * @param errorTypeRepository the error type repository
   * @return the found exception or empty.
   */
  private static Optional<Exception> findRootExceptionForErrorHandling(Exception exception, Processor processor,
                                                                       ErrorTypeLocator errorTypeLocator,
                                                                       ErrorTypeRepository errorTypeRepository) {
    List<Throwable> causesAsList = getExceptionsAsList(exception);
    for (Throwable cause : causesAsList) {
      if (cause instanceof MessagingException && ((MessagingException) cause).getEvent().getError().isPresent()) {
        return of((Exception) cause);
      }
    }
    int causeIndex = 0;
    for (Throwable cause : causesAsList) {
      if (cause instanceof TypedException) {
        return of((Exception) cause);
      }
      if (cause instanceof MuleException || cause instanceof MuleRuntimeException) {
        ErrorType errorType = errorTypeLocator.lookupErrorType(cause);
        ErrorType unknownErrorType = errorTypeRepository.getErrorType(UNKNOWN).get();
        if (!errorType.equals(unknownErrorType)) {
          // search for a more specific wrapper first
          int nextCauseIndex = causeIndex + 1;
          ComponentLocation componentLocation =
              processor instanceof AnnotatedObject ? ((AnnotatedObject) processor).getLocation() : null;
          if (causesAsList.size() > nextCauseIndex && componentLocation != null) {
            Throwable causeOwnerException = causesAsList.get(nextCauseIndex);
            ErrorType causeOwnerErrorType;
            if (causeOwnerException instanceof TypedException) {
              causeOwnerErrorType = ((TypedException) causeOwnerException).getErrorType();
            } else {
              causeOwnerErrorType = errorTypeLocator
                  .lookupComponentErrorType(componentLocation.getComponentIdentifier().getIdentifier(), causeOwnerException);
            }
            if (!unknownErrorType.equals(causeOwnerErrorType)
                && new SingleErrorTypeMatcher(errorType).match(causeOwnerErrorType)) {
              return of((Exception) causeOwnerException);
            }
          }
          return of((Exception) cause);
        }
      }
      causeIndex++;
    }
    return empty();
  }

  static boolean errorCauseMatchesException(Throwable causeException, Optional<Error> error) {
    Throwable throwable = causeException instanceof TypedException ? causeException.getCause() : causeException;
    return !error.get().getCause().equals(throwable);
  }

  public static Error getErrorFromFailingProcessor(Object annotatedObject, Throwable causeException,
                                                   ErrorTypeLocator errorTypeLocator) {
    ErrorType errorType = getErrorTypeFromFailingProcessor(annotatedObject, causeException, errorTypeLocator);
    if (causeException instanceof TypedException) {
      causeException = causeException.getCause();
    }
    return ErrorBuilder.builder(causeException).errorType(errorType).build();
  }

  /**
   * Resolve the root cause of an exception. If the exception is an instance of {@link ErrorMessageAwareException} then it's root
   * cause is used, else the candidate exception instance if returned.
   * 
   * @param exception candidate exception.
   * @return root cause exception.
   */
  public static Throwable getRootCauseException(Throwable exception) {
    return exception instanceof ErrorMessageAwareException ? ((ErrorMessageAwareException) exception).getRootCause() : exception;
  }

  public static MessagingException updateMessagingExceptionWithError(MessagingException exception, Processor failing,
                                                                     FlowConstruct flowConstruct) {
    // If Event already has Error, for example because of an interceptor then conserve existing Error instance
    if (!exception.getEvent().getError().isPresent()) {
      exception
          .setProcessedEvent(createErrorEvent(exception.getEvent(), failing, exception,
                                              flowConstruct.getMuleContext().getErrorTypeLocator()));
    }
    return putContext(exception, failing, exception.getEvent(), flowConstruct, flowConstruct.getMuleContext());
  }
}
