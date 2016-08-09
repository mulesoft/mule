/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.client;

import static org.mule.runtime.core.api.client.SimpleOptionsBuilder.newOptions;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.client.OperationOptions;
import org.mule.runtime.core.api.connector.ConnectorOperationLocator;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;

import java.io.Serializable;
import java.util.Map;

public class DefaultLocalMuleClient implements MuleClient {

  protected final MuleContext muleContext;
  private ConnectorOperationLocator connectorOperatorLocator;

  public DefaultLocalMuleClient(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  protected ConnectorOperationLocator getConnectorMessageProcessLocator() {
    if (connectorOperatorLocator == null) {
      this.connectorOperatorLocator = muleContext.getRegistry().get(OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR);
      if (this.connectorOperatorLocator == null) {
        throw new MuleRuntimeException(CoreMessages.createStaticMessage("Could not find required %s in the registry under key %s",
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
  public MuleMessage send(String url, Object payload, Map<String, Serializable> messageProperties) throws MuleException {
    return send(url, createMessage(payload, messageProperties));
  }

  @Override
  public MuleMessage send(String url, MuleMessage message) throws MuleException {
    final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator()
        .locateConnectorOperation(url, newOptions().outbound().build(), MessageExchangePattern.REQUEST_RESPONSE);
    if (connectorMessageProcessor != null) {
      return returnMessage(connectorMessageProcessor.process(createRequestResponseMuleEvent(message)));
    } else {
      throw createUnsupportedUrlException(url);
    }
  }

  @Override
  public MuleMessage send(String url, MuleMessage message, OperationOptions operationOptions) throws MuleException {
    final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator()
        .locateConnectorOperation(url, operationOptions, MessageExchangePattern.REQUEST_RESPONSE);
    if (connectorMessageProcessor != null) {
      return returnMessage(connectorMessageProcessor.process(createRequestResponseMuleEvent(message)));
    } else {
      throw createUnsupportedUrlException(url);
    }
  }

  private MuleException createUnsupportedUrlException(String url) {
    return new DefaultMuleException("No installed connector supports url " + url);
  }

  @Override
  public MuleMessage send(String url, Object payload, Map<String, Serializable> messageProperties, long timeout)
      throws MuleException {
    return send(url, createMessage(payload, messageProperties), timeout);

  }

  @Override
  public MuleMessage send(String url, MuleMessage message, long timeout) throws MuleException {
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
        .locateConnectorOperation(url, newOptions().outbound().build(), MessageExchangePattern.ONE_WAY);
    if (connectorMessageProcessor != null) {
      connectorMessageProcessor.process(createOneWayMuleEvent(message));
    } else {
      throw createUnsupportedUrlException(url);
    }
  }

  @Override
  public void dispatch(String url, MuleMessage message, OperationOptions operationOptions) throws MuleException {
    final MessageProcessor connectorMessageProcessor =
        getConnectorMessageProcessLocator().locateConnectorOperation(url, operationOptions, MessageExchangePattern.ONE_WAY);
    if (connectorMessageProcessor != null) {
      connectorMessageProcessor.process(createOneWayMuleEvent(message));
    } else {
      dispatch(url, message);
    }
  }

  @Override
  public MuleMessage request(String url, long timeout) throws MuleException {
    final OperationOptions operationOptions = newOptions().responseTimeout(timeout).build();
    final MessageProcessor connectorMessageProcessor =
        getConnectorMessageProcessLocator().locateConnectorOperation(url, operationOptions, MessageExchangePattern.ONE_WAY);
    if (connectorMessageProcessor != null) {
      final MuleEvent event =
          connectorMessageProcessor.process(createOneWayMuleEvent(MuleMessage.builder().nullPayload().build()));

      return event == null || event instanceof VoidMuleEvent ? null : event.getMessage();
    } else {
      return null;
    }
  }

  protected MuleEvent createRequestResponseMuleEvent(MuleMessage message) throws MuleException {
    return new DefaultMuleEvent(message, MessageExchangePattern.REQUEST_RESPONSE, new MuleClientFlowConstruct(muleContext));
  }

  protected MuleEvent createOneWayMuleEvent(MuleMessage message) throws MuleException {
    return new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, new MuleClientFlowConstruct(muleContext));
  }

  protected MuleMessage returnMessage(MuleEvent event) {
    if (event != null && !VoidMuleEvent.getInstance().equals(event)) {
      return event.getMessage();
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
