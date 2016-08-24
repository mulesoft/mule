/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.client;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.DefaultMessageContext.create;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.client.SimpleOptionsBuilder.newOptions;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.functional.Either.left;
import static org.mule.runtime.core.functional.Either.right;
import static org.mule.runtime.core.message.ErrorBuilder.builder;

import java.util.Optional;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.client.OperationOptions;
import org.mule.runtime.core.api.connector.ConnectorOperationLocator;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.functional.Either;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;

import java.io.Serializable;
import java.util.Map;

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
  public Either<Error, MuleMessage> send(String url, Object payload, Map<String, Serializable> messageProperties)
      throws MuleException {
    return send(url, createMessage(payload, messageProperties));
  }

  @Override
  public Either<Error, MuleMessage> send(String url, MuleMessage message) throws MuleException {
    final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator()
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

  private Either<Error, MuleMessage> createEitherResult(MuleEvent muleEvent) {
    if (muleEvent == null) {
      //This should never return a null event. This happen because of mule 3.x behaviour with filters.
      //We will just return an error in this case.
      return left(builder(new MuleRuntimeException(createStaticMessage(MESSAGE_FILTERED_ERROR_MESSAGE))).build());
    }
    if (muleEvent.getError() == null) {
      return right(muleEvent.getMessage());
    }
    return left(muleEvent.getError());
  }

  @Override
  public Either<Error, MuleMessage> send(String url, MuleMessage message, OperationOptions operationOptions)
      throws MuleException {
    final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator()
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
  public Either<Error, MuleMessage> send(String url, Object payload, Map<String, Serializable> messageProperties, long timeout)
      throws MuleException {
    return send(url, createMessage(payload, messageProperties), timeout);

  }

  @Override
  public Either<Error, MuleMessage> send(String url, MuleMessage message, long timeout) throws MuleException {
    return send(url, message, newOptions().outbound().responseTimeout(timeout).build());
  }

  protected MuleMessage createMessage(Object payload, Map<String, Serializable> messageProperties) {
    final Builder builder = MuleMessage.builder().payload(payload);
    if (messageProperties != null) {
      builder.outboundProperties(messageProperties);
    }
    return builder.build();
  }

  @Override
  public void dispatch(String url, MuleMessage message) throws MuleException {
    final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator()
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
  public void dispatch(String url, MuleMessage message, OperationOptions operationOptions) throws MuleException {
    final MessageProcessor connectorMessageProcessor =
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
  public Either<Error, Optional<MuleMessage>> request(String url, long timeout) throws MuleException {
    final OperationOptions operationOptions = newOptions().responseTimeout(timeout).build();
    final MessageProcessor connectorMessageProcessor =
        getConnectorMessageProcessLocator().locateConnectorOperation(url, operationOptions, ONE_WAY);
    if (connectorMessageProcessor != null) {
      if (connectorMessageProcessor instanceof FlowConstructAware) {
        ((FlowConstructAware) connectorMessageProcessor).setFlowConstruct(flowConstruct);
      }
      final MuleEvent event =
          connectorMessageProcessor.process(createOneWayMuleEvent(MuleMessage.builder().nullPayload().build()));
      if (event == null || event instanceof VoidMuleEvent) {
        return right(empty());
      }
      if (event.getError() != null) {
        return left(event.getError());
      }
      return right(ofNullable(event.getMessage()));
    } else {
      return right(empty());
    }
  }

  protected MuleEvent createRequestResponseMuleEvent(MuleMessage message) throws MuleException {
    return new DefaultMuleEvent(create(flowConstruct, "muleClient"), message, REQUEST_RESPONSE, flowConstruct);
  }

  protected MuleEvent createOneWayMuleEvent(MuleMessage message) throws MuleException {
    return new DefaultMuleEvent(create(flowConstruct, "muleClient"), message, ONE_WAY, flowConstruct);
  }

  protected MuleEvent returnEvent(MuleEvent event) {
    if (event != null && !VoidMuleEvent.getInstance().equals(event)) {
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
