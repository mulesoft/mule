/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.util.Collections.singletonList;
import static org.mule.runtime.core.routing.MapSplitter.MAP_ENTRY_KEY;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.expression.ExpressionConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.NodeList;

/**
 * Splits a message using the expression provided invoking the next message processor one for each split part.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www.eaipatterns.com/Sequencer.html</a>
 */
public class ExpressionSplitter extends AbstractSplitter implements Initialisable {

  protected ExpressionManager expressionManager;
  protected ExpressionConfig config = new ExpressionConfig();

  public ExpressionSplitter() {
    // Used by spring
  }

  public ExpressionSplitter(ExpressionConfig config) {
    this.config = config;
  }

  @Override
  public void initialise() throws InitialisationException {
    expressionManager = muleContext.getExpressionManager();
    config.validate(expressionManager);
  }

  @Override
  protected List<MuleEvent> splitMessage(MuleEvent event) {
    Object result =
        muleContext.getExpressionManager().evaluate(config.getFullExpression(expressionManager), event, flowConstruct);
    if (result instanceof Object[]) {
      result = Arrays.asList((Object[]) result);
    }
    if (result instanceof Iterable<?>) {
      List<MuleEvent> messages = new ArrayList<>();
      ((Iterable<?>) result).iterator()
          .forEachRemaining(value -> messages
              .add(MuleEvent.builder(event).message(MuleMessage.builder().payload(value).build()).build()));
      return messages;
    } else if (result instanceof Map<?, ?>) {
      List<MuleEvent> list = new LinkedList<>();
      Set<Map.Entry<?, ?>> set = ((Map) result).entrySet();
      for (Entry<?, ?> entry : set) {
        MuleEvent newEvent = MuleEvent.builder(event).message(MuleMessage.builder().payload(entry.getValue()).build())
            .addFlowVariable(MAP_ENTRY_KEY, entry.getKey()).build();
        list.add(newEvent);
      }
      return list;
    } else if (result instanceof MuleMessage) {
      return singletonList(MuleEvent.builder(event).message((MuleMessage) result).build());
    } else if (result instanceof NodeList) {
      NodeList nodeList = (NodeList) result;
      List<MuleEvent> messages = new ArrayList<>(nodeList.getLength());
      for (int i = 0; i < nodeList.getLength(); i++) {
        messages.add(MuleEvent.builder(event).message(MuleMessage.builder().payload(nodeList.item(i)).build()).build());
      }
      return messages;
    } else if (result == null) {
      return new ArrayList<>();
    } else {
      logger.info("The expression does not evaluate to a type that can be split: " + result.getClass().getName());
      return singletonList(MuleEvent.builder(event).message(MuleMessage.builder().payload(result).build()).build());
    }
  }

  public String getExpression() {
    return config.getExpression();
  }

  public void setExpression(String expression) {
    this.config.setExpression(expression);
  }

}
