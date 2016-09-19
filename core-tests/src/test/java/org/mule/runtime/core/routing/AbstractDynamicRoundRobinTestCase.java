/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;


import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

public class AbstractDynamicRoundRobinTestCase extends AbstractMuleContextTestCase {

  protected static final String LETTER_A = "a";
  protected static final String LETTER_B = "b";
  protected static final String LETTER_C = "c";
  protected static final String ID_1 = "ID_1";
  protected static final String ID_2 = "ID_2";
  protected static final String EXCEPTION_MESSAGE = "Failure!";
  protected static final String ID_PROPERTY_NAME = "id";

  protected List<Processor> getMessageProcessorsList() {
    List<Processor> messageProcessors = new ArrayList<>();
    messageProcessors.add(new LetterMessageProcessor(LETTER_A));
    messageProcessors.add(new LetterMessageProcessor(LETTER_B));
    messageProcessors.add(new LetterMessageProcessor(LETTER_C));
    return messageProcessors;
  }

  protected List<Processor> getMessageProcessorsListWithFailingMessageProcessor() {
    List<Processor> messageProcessors = new ArrayList<>();
    messageProcessors.add(new LetterMessageProcessor(LETTER_A));
    messageProcessors.add(event -> {
      throw new DefaultMuleException(CoreMessages.createStaticMessage(EXCEPTION_MESSAGE));
    });
    messageProcessors.add(new LetterMessageProcessor(LETTER_B));
    return messageProcessors;

  }

  protected IdentifiableDynamicRouteResolver getIdentifiableDynamicRouteResolver() {
    return new IdentifiableDynamicRouteResolver() {

      @Override
      public String getRouteIdentifier(Event event) throws MessagingException {
        return (String) event.getVariable(ID_PROPERTY_NAME).getValue();
      }

      @Override
      public List<Processor> resolveRoutes(Event event) throws MessagingException {
        return getMessageProcessorsList();
      }

    };
  }

  protected DynamicRouteResolver getDynamicRouteResolver() {
    return event -> getMessageProcessorsList();
  }

  protected Event getEventWithId(String id) throws Exception {
    return Event.builder(DefaultEventContext.create(MuleTestUtils.getTestFlow(muleContext), TEST_CONNECTOR))
        .message(InternalMessage.builder().payload(TEST_MESSAGE).build())
        .exchangePattern(MessageExchangePattern.REQUEST_RESPONSE)
        .addVariable(ID_PROPERTY_NAME, id)
        .build();
  }

  public static class LetterMessageProcessor implements Processor {

    private String letter;

    public LetterMessageProcessor(String letter) {
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

}
