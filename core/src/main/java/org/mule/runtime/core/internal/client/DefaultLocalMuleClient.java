/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.client;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.api.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.client.SimpleOptionsBuilder.newOptions;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.api.message.ErrorBuilder.builder;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.client.MuleClientFlowConstruct;
import org.mule.runtime.core.api.client.OperationOptions;
import org.mule.runtime.core.api.connector.ConnectorOperationLocator;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.message.InternalMessage.Builder;

import java.io.Serializable;
import java.util.Map;
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
  public Either<Error, Message> send(String url, Object payload, Map<String, Serializable> messageProperties)
      throws MuleException {
    return send(url, createMessage(payload, messageProperties));
  }

  @Override
  public Either<Error, Message> send(String url, Message message) throws MuleException {
    final Processor connectorMessageProcessor = getConnectorMessageProcessLocator()
        .locateConnectorOperation(url, newOptions().outbound().build(), REQUEST_RESPONSE);
    if (connectorMessageProcessor != null) {
      return createEitherResult(connectorMessageProcessor.process(createMuleEvent(message)));
    } else {
      throw createUnsupportedUrlException(url);
    }
  }

  private Either<Error, Message> createEitherResult(Event muleEvent) {
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
  public Either<Error, Message> send(String url, Message message, OperationOptions operationOptions)
      throws MuleException {
    final Processor connectorMessageProcessor = getConnectorMessageProcessLocator()
        .locateConnectorOperation(url, operationOptions, REQUEST_RESPONSE);
    if (connectorMessageProcessor != null) {
      return createEitherResult(returnEvent(connectorMessageProcessor.process(createMuleEvent(message))));
    } else {
      throw createUnsupportedUrlException(url);
    }
  }

  private MuleException createUnsupportedUrlException(String url) {
    return new DefaultMuleException("No installed connector supports url " + url);
  }

  @Override
  public Either<Error, Message> send(String url, Object payload, Map<String, Serializable> messageProperties,
                                     long timeout)
      throws MuleException {
    return send(url, createMessage(payload, messageProperties), timeout);

  }

  @Override
  public Either<Error, Message> send(String url, Message message, long timeout) throws MuleException {
    return send(url, message, newOptions().outbound().responseTimeout(timeout).build());
  }

  protected Message createMessage(Object payload, Map<String, Serializable> messageProperties) {
    final Builder builder = InternalMessage.builder().value(payload);
    if (messageProperties != null) {
      builder.outboundProperties(messageProperties);
    }
    return builder.build();
  }

  @Override
  public void dispatch(String url, Message message) throws MuleException {
    final Processor connectorMessageProcessor = getConnectorMessageProcessLocator()
        .locateConnectorOperation(url, newOptions().outbound().build(), ONE_WAY);
    if (connectorMessageProcessor != null) {
      connectorMessageProcessor.process(createMuleEvent(message));
    } else {
      throw createUnsupportedUrlException(url);
    }
  }

  @Override
  public void dispatch(String url, Message message, OperationOptions operationOptions) throws MuleException {
    final Processor connectorMessageProcessor =
        getConnectorMessageProcessLocator().locateConnectorOperation(url, operationOptions, ONE_WAY);
    if (connectorMessageProcessor != null) {
      connectorMessageProcessor.process(createMuleEvent(message));
    } else {
      dispatch(url, message);
    }
  }

  @Override
  public Either<Error, Optional<Message>> request(String url, long timeout) throws MuleException {
    final OperationOptions operationOptions = newOptions().responseTimeout(timeout).build();
    final Processor connectorMessageProcessor =
        getConnectorMessageProcessLocator().locateConnectorOperation(url, operationOptions, ONE_WAY);
    if (connectorMessageProcessor != null) {
      final Event event = connectorMessageProcessor.process(createMuleEvent(of(null)));
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

  protected Event createMuleEvent(Message message) throws MuleException {
    return baseEventBuilder(message).build();
  }

  private org.mule.runtime.core.api.Event.Builder baseEventBuilder(Message message) {
    return Event.builder(create(flowConstruct, fromSingleComponent("muleClient"))).message(message).flow(flowConstruct);
  }

  protected Event returnEvent(Event event) {
    if (event != null) {
      return event;
    } else {
      return null;
    }
  }

}
