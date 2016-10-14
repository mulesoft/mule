/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.client;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.client.SimpleOptionsBuilder.newOptions;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR;
import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.functional.Either.left;
import static org.mule.runtime.core.functional.Either.right;
import static org.mule.runtime.core.message.ErrorBuilder.builder;

import java.io.Serializable;
import java.util.Map;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.message.InternalMessage.Builder;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.client.OperationOptions;
import org.mule.runtime.core.api.connector.ConnectorOperationLocator;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.functional.Either;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;

import java.util.Optional;

public class DefaultLocalMuleClient implements MuleClient {

  public static final String MESSAGE_FILTERED_ERROR_MESSAGE = "message filtered";
  protected final MuleContext muleContext;
  private FlowConstruct flowConstruct;
  private ConnectorOperationLocator connectorOperatorLocator;

  public DefaultLocalMuleClient(MuleContext muleContext) {
    this.muleContext = muleContext;
    this.flowConstruct = new MuleClientFlowConstruct(muleContext);
  }

  protected ConnectorOperationLocator getConnectorMessageProcessLocator() {
    if (connectorOperatorLocator == null) {
      this.connectorOperatorLocator = muleContext.getRegistry().get(OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR);
      if (this.connectorOperatorLocator == null) {
        throw new MuleRuntimeException(createStaticMessage("Could not find required %s in the registry under key %s",
                                                           ConnectorOperationLocator.class.getName(),
                                                           OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR));
      }
    }
    return connectorOperatorLocator;
  }

  @Override
  public void dispatch(String url, Object payload, Map<String, Serializable> messageProperties) throws MuleException {
    dispatch(url, createMessage(payload, messageProperties));
  }

  @Override
  public Either<Error, InternalMessage> send(String url, Object payload, Map<String, Serializable> messageProperties)
      throws MuleException {
    return send(url, createMessage(payload, messageProperties));
  }

  @Override
  public Either<Error, InternalMessage> send(String url, InternalMessage message) throws MuleException {
    final Processor connectorMessageProcessor = getConnectorMessageProcessLocator()
        .locateConnectorOperation(url, newOptions().outbound().build(), REQUEST_RESPONSE);
    if (connectorMessageProcessor != null) {
      if (connectorMessageProcessor instanceof FlowConstructAware) {
        ((FlowConstructAware) connectorMessageProcessor).setFlowConstruct(flowConstruct);
      }
      return createEitherResult(connectorMessageProcessor.process(createRequestResponseMuleEvent(message)));
    } else {
      throw createUnsupportedUrlException(url);
    }
  }

  private Either<Error, InternalMessage> createEitherResult(Event muleEvent) {
    if (muleEvent == null) {
      // This should never return a null event. This happen because of mule 3.x behaviour with filters.
      // We will just return an error in this case.
      ErrorType anyErrorType = muleContext.getErrorTypeRepository().getAnyErrorType();
      return left(builder(new MuleRuntimeException(createStaticMessage(MESSAGE_FILTERED_ERROR_MESSAGE))).errorType(anyErrorType)
          .build());
    }
    if (!muleEvent.getError().isPresent()) {
      return right(muleEvent.getMessage());
    }
    return left(muleEvent.getError().get());
  }

  @Override
  public Either<Error, InternalMessage> send(String url, InternalMessage message, OperationOptions operationOptions)
      throws MuleException {
    final Processor connectorMessageProcessor = getConnectorMessageProcessLocator()
        .locateConnectorOperation(url, operationOptions, REQUEST_RESPONSE);
    if (connectorMessageProcessor != null) {
      if (connectorMessageProcessor instanceof FlowConstructAware) {
        ((FlowConstructAware) connectorMessageProcessor).setFlowConstruct(flowConstruct);
      }
      return createEitherResult(returnEvent(connectorMessageProcessor.process(createRequestResponseMuleEvent(message))));
    } else {
      throw createUnsupportedUrlException(url);
    }
  }

  private MuleException createUnsupportedUrlException(String url) {
    return new DefaultMuleException("No installed connector supports url " + url);
  }

  @Override
  public Either<Error, InternalMessage> send(String url, Object payload, Map<String, Serializable> messageProperties,
                                             long timeout)
      throws MuleException {
    return send(url, createMessage(payload, messageProperties), timeout);

  }

  @Override
  public Either<Error, InternalMessage> send(String url, InternalMessage message, long timeout) throws MuleException {
    return send(url, message, newOptions().outbound().responseTimeout(timeout).build());
  }

  protected InternalMessage createMessage(Object payload, Map<String, Serializable> messageProperties) {
    final Builder builder = InternalMessage.builder().payload(payload);
    if (messageProperties != null) {
      builder.outboundProperties(messageProperties);
    }
    return builder.build();
  }

  @Override
  public void dispatch(String url, InternalMessage message) throws MuleException {
    final Processor connectorMessageProcessor = getConnectorMessageProcessLocator()
        .locateConnectorOperation(url, newOptions().outbound().build(), ONE_WAY);
    if (connectorMessageProcessor != null) {
      if (connectorMessageProcessor instanceof FlowConstructAware) {
        ((FlowConstructAware) connectorMessageProcessor).setFlowConstruct(flowConstruct);
      }
      connectorMessageProcessor.process(createOneWayMuleEvent(message));
    } else {
      throw createUnsupportedUrlException(url);
    }
  }

  @Override
  public void dispatch(String url, InternalMessage message, OperationOptions operationOptions) throws MuleException {
    final Processor connectorMessageProcessor =
        getConnectorMessageProcessLocator().locateConnectorOperation(url, operationOptions, ONE_WAY);
    if (connectorMessageProcessor != null) {
      if (connectorMessageProcessor instanceof FlowConstructAware) {
        ((FlowConstructAware) connectorMessageProcessor).setFlowConstruct(flowConstruct);
      }
      connectorMessageProcessor.process(createOneWayMuleEvent(message));
    } else {
      dispatch(url, message);
    }
  }

  @Override
  public Either<Error, Optional<InternalMessage>> request(String url, long timeout) throws MuleException {
    final OperationOptions operationOptions = newOptions().responseTimeout(timeout).build();
    final Processor connectorMessageProcessor =
        getConnectorMessageProcessLocator().locateConnectorOperation(url, operationOptions, ONE_WAY);
    if (connectorMessageProcessor != null) {
      if (connectorMessageProcessor instanceof FlowConstructAware) {
        ((FlowConstructAware) connectorMessageProcessor).setFlowConstruct(flowConstruct);
      }
      final Event event =
          connectorMessageProcessor.process(createOneWayMuleEvent(InternalMessage.builder().nullPayload().build()));
      if (event == null) {
        return right(empty());
      }
      if (event.getError().isPresent()) {
        return left(event.getError().get());
      }
      return right(ofNullable(event.getMessage()));
    } else {
      return right(empty());
    }
  }

  protected Event createRequestResponseMuleEvent(InternalMessage message) throws MuleException {
    return baseEventBuilder(message).exchangePattern(REQUEST_RESPONSE).build();
  }

  protected Event createOneWayMuleEvent(InternalMessage message) throws MuleException {
    return baseEventBuilder(message).exchangePattern(ONE_WAY).build();
  }

  private org.mule.runtime.core.api.Event.Builder baseEventBuilder(InternalMessage message) {
    return Event.builder(create(flowConstruct, "muleClient")).message(message).flow(flowConstruct);
  }

  protected Event returnEvent(Event event) {
    if (event != null) {
      return event;
    } else {
      return null;
    }
  }

  /**
   * Placeholder class which makes the default exception handler available.
   */
  static public class MuleClientFlowConstruct implements FlowConstruct {

    MuleContext muleContext;

    public MuleClientFlowConstruct(MuleContext muleContext) {
      this.muleContext = muleContext;
    }

    @Override
    public String getName() {
      return "MuleClient";
    }

    @Override
    public MessagingExceptionHandler getExceptionListener() {
      return new DefaultMessagingExceptionStrategy(muleContext);
    }

    @Override
    public LifecycleState getLifecycleState() {
      return null;
    }

    @Override
    public FlowConstructStatistics getStatistics() {
      return null;
    }

    @Override
    public MuleContext getMuleContext() {
      return muleContext;
    }

    public MessageProcessorChain getMessageProcessorChain() {
      return null;
    }
  }
}
