/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A holder for a pair of MessageProcessor and an expression.
 */
public class MessageProcessorExpressionPair extends AbstractComponent
    implements MuleContextAware, Lifecycle {

  private final static Logger LOGGER = LoggerFactory.getLogger(MessageProcessorExpressionPair.class);

  private final String expression;
  private final Processor messageProcessor;

  private MuleContext muleContext;

  public MessageProcessorExpressionPair(String expression, Processor messageProcessor) {
    requireNonNull(expression, "expression can't be null");
    requireNonNull(messageProcessor, "messageProcessor can't be null");
    this.expression = expression;
    this.messageProcessor = messageProcessor;
  }

  public String getExpression() {
    return expression;
  }

  public Processor getMessageProcessor() {
    return messageProcessor;
  }

  @Override
  public String toString() {
    return reflectionToString(this, SHORT_PREFIX_STYLE);
  }

  // This class being just a logic-less tuple, it directly delegates lifecyle
  // events to its members, without any control.

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    if (messageProcessor instanceof MuleContextAware) {
      ((MuleContextAware) messageProcessor).setMuleContext(context);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(messageProcessor, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(messageProcessor);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(messageProcessor);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(messageProcessor, LOGGER);
  }

}
