/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.routing.IdentifiableDynamicRouteResolver;

import java.util.ArrayList;
import java.util.List;

public class IdentifiableCustomRouteResolver implements IdentifiableDynamicRouteResolver, MuleContextAware {

  private final String ID_EXPRESSION = "#[flowVars['id']]";

  private MuleContext muleContext;

  static List<MessageProcessor> routes = new ArrayList<>();

  @Override
  public List<MessageProcessor> resolveRoutes(MuleEvent event) {
    return routes;
  }

  @Override
  public String getRouteIdentifier(MuleEvent event) throws MessagingException {
    return muleContext.getExpressionManager().parse(ID_EXPRESSION, event, null);
  }

  public static class AddLetterMessageProcessor implements MessageProcessor {

    private String letter;

    public AddLetterMessageProcessor(String letter) {
      this.letter = letter;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      try {
        event.setMessage(MuleMessage.builder(event.getMessage()).payload(letter).build());
        return event;
      } catch (Exception e) {
        throw new DefaultMuleException(e);
      }
    }

  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

}
