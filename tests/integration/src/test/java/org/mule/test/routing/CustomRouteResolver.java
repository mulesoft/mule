/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.routing.DynamicRouteResolver;

import java.util.ArrayList;
import java.util.List;

public class CustomRouteResolver implements DynamicRouteResolver {

  static List<Processor> routes = new ArrayList<>();

  @Override
  public List<Processor> resolveRoutes(Event event) {
    return routes;
  }

  public static class AddLetterMessageProcessor implements Processor {

    private String letter;

    public AddLetterMessageProcessor(String letter) {
      this.letter = letter;
    }

    @Override
    public Event process(Event event) throws MuleException {
      try {
        return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(letter).build()).build();
      } catch (Exception e) {
        throw new DefaultMuleException(e);
      }
    }

  }

  public static class FailingMessageProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new DefaultMuleException(CoreMessages.createStaticMessage(""));
    }
  }

  public static class AddLetterThenFailsMessageProcessor implements Processor, MuleContextAware {

    private String letter;
    private MuleContext muleContext;

    public AddLetterThenFailsMessageProcessor(String letter) {
      this.letter = letter;
    }

    @Override
    public Event process(Event event) throws MuleException {
      try {
        event = Event
            .builder(event).message(
                                    InternalMessage.builder(event.getMessage())
                                        .payload(muleContext.getTransformationService()
                                            .transform(event.getMessage(), DataType.STRING).getPayload().getValue() + letter)
                                        .build())
            .build();
      } catch (Exception e) {
      }
      throw new DefaultMuleException(CoreMessages.createStaticMessage(""));
    }

    @Override
    public void setMuleContext(MuleContext context) {
      this.muleContext = context;
    }
  }
}
