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
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessorChain;

/**
 * A holder for a pair of MessageProcessor and an expression.
 */
public class MessageProcessorExpressionPair extends AbstractAnnotatedObject
    implements MuleContextAware, Lifecycle {

  private final String expression;
  private final MessageProcessorChain messageProcessor;

  public MessageProcessorExpressionPair(String expression, MessageProcessorChain messageProcessor) {
    requireNonNull(expression, "expression can't be null");
    requireNonNull(messageProcessor, "messageProcessor can't be null");
    this.expression = expression;
    this.messageProcessor = messageProcessor;
  }

  public String getExpression() {
    return expression;
  }

  public MessageProcessorChain getMessageProcessor() {
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
    messageProcessor.setMuleContext(context);
  }

  @Override
  public void initialise() throws InitialisationException {
    messageProcessor.initialise();
  }

  @Override
  public void start() throws MuleException {
    messageProcessor.start();
  }

  @Override
  public void stop() throws MuleException {
    messageProcessor.stop();
  }

  @Override
  public void dispose() {
    messageProcessor.dispose();
  }

}
