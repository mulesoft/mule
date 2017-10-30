/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static org.apache.commons.lang3.SystemUtils.LINE_SEPARATOR;
import static org.mule.functional.api.notification.FunctionalTestNotification.EVENT_RECEIVED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.core.api.util.StringMessageUtils.getBoilerPlate;
import static org.mule.runtime.core.api.util.StringMessageUtils.truncate;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.functional.api.notification.FunctionalTestNotification;
import org.mule.functional.api.notification.FunctionalTestNotificationListener;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.Message.Builder;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

/**
 * <code>FunctionalTestProcessor</code> is a service that can be used by functional tests. This service accepts an EventCallback
 * that can be used to assert the state of the current event.
 * <p/>
 * Also, this service fires {@link FunctionalTestNotification} via Mule for every message received. Tests can register with Mule
 * to receive these events by implementing {@link FunctionalTestNotificationListener}.
 *
 * @see FunctionalTestNotification
 * @see FunctionalTestNotificationListener
 */
public class FunctionalTestProcessor extends AbstractComponent implements Processor, Lifecycle {

  private static final Logger LOGGER = getLogger(FunctionalTestProcessor.class);

  @Inject
  private MuleContext muleContext;

  @Inject
  private ExtendedExpressionManager expressionManager;

  @Inject
  private NotificationDispatcher notificationFirer;

  private EventCallback eventCallback;
  private Object returnData = null;
  private boolean throwException = false;
  private Class<? extends Throwable> exceptionToThrow;
  private String exceptionText = "";
  private boolean enableMessageHistory = true;
  private boolean enableNotifications = true;
  private String appendString;
  private long waitTime = 0;
  private boolean logMessageDetails = false;
  private String id = "<none>";
  private String processorClass;
  private Processor processor;
  private static List<LifecycleCallback> lifecycleCallbacks = new ArrayList<>();


  /**
   * Keeps a list of any messages received on this service. Note that only references to the messages (objects) are stored, so any
   * subsequent changes to the objects will change the history.
   */
  private List<CoreEvent> messageHistory;


  @Override
  public void initialise() throws InitialisationException {
    if (enableMessageHistory) {
      messageHistory = new CopyOnWriteArrayList<>();
    }
    for (LifecycleCallback callback : lifecycleCallbacks) {
      callback.onTransition(id, Initialisable.PHASE_NAME);
    }

    if (processorClass != null) {
      try {
        processor = (Processor) instantiateClass(processorClass);
        initialiseIfNeeded(processor, true, muleContext);
      } catch (Exception e) {
        throw new InitialisationException(e, this);
      }
    }
  }

  @Override
  public void start() throws MuleException {
    for (LifecycleCallback callback : lifecycleCallbacks) {
      callback.onTransition(id, Startable.PHASE_NAME);
    }

    if (processor != null) {
      startIfNeeded(processor);
    }
  }

  @Override
  public void stop() throws MuleException {
    for (LifecycleCallback callback : lifecycleCallbacks) {
      callback.onTransition(id, Stoppable.PHASE_NAME);
    }

    if (processor != null) {
      stopIfNeeded(processor);
    }
  }

  @Override
  public void dispose() {
    for (LifecycleCallback callback : lifecycleCallbacks) {
      callback.onTransition(id, Disposable.PHASE_NAME);
    }

    if (processor != null) {
      disposeIfNeeded(processor, LOGGER);
    }
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    try {
      if (isThrowException()) {
        throwException();
      }
      return doProcess(event);
    } catch (Throwable t) {
      if (t instanceof MuleException) {
        throw (MuleException) t;
      } else if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      } else if (t instanceof Error) {
        throw (Error) t;
      } else {
        throw new DefaultMuleException(t);
      }
    }
  }

  /**
   * Always throws a {@link FunctionalTestException}. This methodis only called if {@link #isThrowException()} is true.
   *
   * @throws FunctionalTestException or the exception specified in 'exceptionType
   */
  protected void throwException() throws Exception {
    if (getExceptionToThrow() != null) {
      if (StringUtils.isNotBlank(exceptionText)) {
        Throwable exception = instantiateClass(getExceptionToThrow(), new Object[] {exceptionText});
        throw (Exception) exception;
      } else {
        throw (Exception) getExceptionToThrow().newInstance();
      }
    } else {
      if (StringUtils.isNotBlank(exceptionText)) {
        throw new FunctionalTestException(exceptionText);
      } else {
        throw new FunctionalTestException();
      }
    }
  }

  /**
   * Will append the value of {@link #getAppendString()} to the contents of the message. This has a side affect that the inbound
   * message will be converted to a string and the return payload will be a string. Note that the value of
   * {@link #getAppendString()} can contain expressions.
   *
   * @param contents the string vlaue of the current message payload
   * @param event the current event
   * @return a concatenated string of the current payload and the appendString
   */
  protected String append(String contents, CoreEvent event) {
    return contents + expressionManager.parse(appendString, event, getLocation());
  }

  /**
   * The service method that implements the test component logic.
   *
   * @param event the current {@link CoreEvent}
   * @return a new message payload according to the configuration of the component
   * @throws Exception if there is a general failure or if {@link #isThrowException()} is true.
   */
  protected CoreEvent doProcess(CoreEvent event) throws Exception {
    if (enableMessageHistory) {
      messageHistory.add(event);
    }

    final Message message = event.getMessage();
    if (LOGGER.isInfoEnabled()) {
      String msg = getBoilerPlate("Message Received in flow: "
          + getLocation().getRootContainerName() + ". Content is: "
          + truncate(message.getPayload().getValue().toString(), 100, true), '*', 80);

      LOGGER.info(msg);
    }

    if (isLogMessageDetails() && LOGGER.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();

      sb.append("Full Message: ").append(LINE_SEPARATOR);
      sb.append(message.getPayload().getValue().toString()).append(LINE_SEPARATOR);
      sb.append(message.toString());
      LOGGER.info(sb.toString());
    }

    if (eventCallback != null) {
      eventCallback.eventReceived(event, this, muleContext);
    }

    if (processor != null) {
      return processor.process(event);
    }

    Builder replyBuilder = Message.builder(message);
    if (returnData != null) {
      if (returnData instanceof String && expressionManager.isExpression(returnData.toString())) {
        replyBuilder =
            replyBuilder.value(expressionManager.parse(returnData.toString(), event, getLocation()));
      } else {
        replyBuilder = replyBuilder.value(returnData);
      }
    } else {
      if (appendString != null) {
        replyBuilder = replyBuilder.value(append(muleContext.getTransformationService()
            .transform(event.getMessage(), DataType.STRING).getPayload().getValue().toString(), event));
      }
    }
    CoreEvent replyMessage = CoreEvent.builder(event).message(replyBuilder.build()).build();

    if (isEnableNotifications()) {
      notificationFirer
          .dispatch(new FunctionalTestNotification(message, getLocation().getRootContainerName(), replyMessage,
                                                   EVENT_RECEIVED));
    }

    // Time to wait before returning
    if (waitTime > 0) {
      try {
        Thread.sleep(waitTime);
      } catch (InterruptedException e) {
        LOGGER.info("FunctionalTestProcessor waitTime was interrupted");
      }
    }
    return replyMessage;
  }

  /**
   * An event callback is called when a message is received by the service. An Event callback isn't strictly required but it is
   * useful for performing assertions on the current message being received. Note that the FunctionalTestProcessor should be made
   * a singleton when using Event callbacks
   * <p/>
   * Another option is to register a {@link FunctionalTestNotificationListener} with Mule and this will deleiver a
   * {@link FunctionalTestNotification} for every message received by this service
   *
   * @return the callback to call when a message is received
   * @see FunctionalTestNotification
   * @see FunctionalTestNotificationListener
   */
  public EventCallback getEventCallback() {
    return eventCallback;
  }

  /**
   * An event callback is called when a message is received by the service. An Event callback isn't strictly required but it is
   * useful for performing assertions on the current message being received. Note that the FunctionalTestProcessor should be made
   * a singleton when using Event callbacks
   * <p/>
   * Another option is to register a {@link FunctionalTestNotificationListener} with Mule and this will deleiver a
   * {@link FunctionalTestNotification} for every message received by this service
   *
   * @param eventCallback the callback to call when a message is received
   * @see FunctionalTestNotification
   * @see FunctionalTestNotificationListener
   */
  public void setEventCallback(EventCallback eventCallback) {
    this.eventCallback = eventCallback;
  }

  /**
   * Often you will may want to return a fixed message payload to simulate and external system call. This can be done using the
   * 'returnData' property. Note that you can return complex objects by using the <container-property> element in the Xml
   * configuration.
   *
   * @return the message payload to always return from this service instance
   */
  public Object getReturnData() {
    return returnData;
  }

  /**
   * Often you will may want to return a fixed message payload to simulate and external system call. This can be done using the
   * 'returnData' property. Note that you can return complex objects by using the <container-property> element in the Xml
   * configuration.
   *
   * @param returnData the message payload to always return from this service instance
   */
  public void setReturnData(Object returnData) {
    this.returnData = returnData;
  }

  /**
   * Sometimes you will want the service to always throw an exception, if this is the case you can set the 'throwException'
   * property to true.
   *
   * @return throwException true if an exception should always be thrown from this instance. If the {@link #getReturnData()}
   *         property is set and is of type java.lang.Exception, that exception will be thrown.
   */
  public boolean isThrowException() {
    return throwException;
  }

  /**
   * Sometimes you will want the service to always throw an exception, if this is the case you can set the 'throwException'
   * property to true.
   *
   * @param throwException true if an exception should always be thrown from this instance. If the {@link #getReturnData()}
   *        property is set and is of type java.lang.Exception, that exception will be thrown.
   */
  public void setThrowException(boolean throwException) {
    this.throwException = throwException;
  }

  public boolean isEnableMessageHistory() {
    return enableMessageHistory;
  }

  public void setEnableMessageHistory(boolean enableMessageHistory) {
    this.enableMessageHistory = enableMessageHistory;
  }

  /**
   * If enableMessageHistory = true, returns the number of messages received by this service.
   *
   * @return -1 if no message history, otherwise the history size
   */
  public int getReceivedMessagesCount() {
    if (messageHistory != null) {
      return messageHistory.size();
    } else {
      return -1;
    }
  }

  /**
   * If enableMessageHistory = true, returns a message received by the service in chronological order. For example,
   * getReceivedMessage(1) returns the first message received by the service, getReceivedMessage(2) returns the second message
   * received by the service, etc.
   */
  public CoreEvent getReceivedMessage(int number) {
    CoreEvent message = null;
    if (messageHistory != null) {
      if (number <= messageHistory.size()) {
        message = messageHistory.get(number - 1);
      }
    }
    return message;
  }

  /**
   * If enableMessageHistory = true, returns the last message received by the service in chronological order.
   */
  public CoreEvent getLastReceivedMessage() {
    if (messageHistory != null) {
      return messageHistory.get(messageHistory.size() - 1);
    } else {
      return null;
    }
  }

  public String getAppendString() {
    return appendString;
  }

  public void setAppendString(String appendString) {
    this.appendString = appendString;
  }

  public boolean isEnableNotifications() {
    return enableNotifications;
  }

  public void setEnableNotifications(boolean enableNotifications) {
    this.enableNotifications = enableNotifications;
  }

  public Class<? extends Throwable> getExceptionToThrow() {
    return exceptionToThrow;
  }

  public void setExceptionToThrow(Class<? extends Throwable> exceptionToThrow) {
    this.exceptionToThrow = exceptionToThrow;
  }

  public long getWaitTime() {
    return waitTime;
  }

  public void setWaitTime(long waitTime) {
    this.waitTime = waitTime;
  }

  public boolean isLogMessageDetails() {
    return logMessageDetails;
  }

  public void setLogMessageDetails(boolean logMessageDetails) {
    this.logMessageDetails = logMessageDetails;
  }

  public String getExceptionText() {
    return exceptionText;
  }

  public void setExceptionText(String text) {
    exceptionText = text;
  }

  public void setId(String id) {
    this.id = id;
  }

  public static void addLifecycleCallback(LifecycleCallback callback) {
    lifecycleCallbacks.add(callback);
  }

  public static void removeLifecycleCallback(LifecycleCallback callback) {
    lifecycleCallbacks.remove(callback);
  }

  public interface LifecycleCallback {

    void onTransition(String name, String newPhase);
  }

  /**
   * @return the first {@code test:processor} from a flow with the provided name.
   */
  public static FunctionalTestProcessor getFromFlow(ConfigurationComponentLocator locator, String flowName) throws Exception {
    return locator.find(ComponentIdentifier.builder().namespace("test").name("processor").build())
        .stream()
        .filter(c -> flowName.equals(c.getRootContainerLocation().toString()))
        .map(c -> (FunctionalTestProcessor) c)
        .findAny().get();
  }

  public void setProcessorClass(String processorClass) {
    this.processorClass = processorClass;
  }
}
