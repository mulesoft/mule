/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;


import org.mule.MessageExchangePattern;
import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

public class AbstractDynamicRoundRobinTestCase extends AbstractMuleContextTestCase
{
    protected static final String LETTER_A = "a";
    protected static final String LETTER_B = "b";
    protected static final String LETTER_C = "c";
    protected static final String ID_1 = "ID_1";
    protected static final String ID_2 = "ID_2";
    protected static final String EXCEPTION_MESSAGE = "Failure!";
    protected static final String ID_PROPERTY_NAME = "id";

    protected List<MessageProcessor> getMessageProcessorsList()
    {
        List<MessageProcessor> messageProcessors = new ArrayList<MessageProcessor>();
        messageProcessors.add(new LetterMessageProcessor(LETTER_A));
        messageProcessors.add(new LetterMessageProcessor(LETTER_B));
        messageProcessors.add(new LetterMessageProcessor(LETTER_C));
        return messageProcessors;
    }

    protected List<MessageProcessor> getMessageProcessorsListWithFailingMessageProcessor()
    {
        List<MessageProcessor> messageProcessors = new ArrayList<MessageProcessor>();
        messageProcessors.add(new LetterMessageProcessor(LETTER_A));
        messageProcessors.add(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                throw new DefaultMuleException(CoreMessages.createStaticMessage(EXCEPTION_MESSAGE));
            }
        });
        messageProcessors.add(new LetterMessageProcessor(LETTER_B));
        return messageProcessors;

    }

    protected IdentifiableDynamicRouteResolver getIdentifiableDynamicRouteResolver()
    {
        return new IdentifiableDynamicRouteResolver()
        {
            @Override
            public String getRouteIdentifier(MuleEvent event) throws MessagingException
            {
                return event.getMessage().getInvocationProperty(ID_PROPERTY_NAME);
            }

            @Override
            public List<MessageProcessor> resolveRoutes(MuleEvent event) throws MessagingException
            {
                return getMessageProcessorsList();
            }

        };
    }

    protected DynamicRouteResolver getDynamicRouteResolver()
    {
        return new DynamicRouteResolver()
        {
            @Override
            public List<MessageProcessor> resolveRoutes(MuleEvent event) throws MessagingException
            {
                return getMessageProcessorsList();
            }
        };
    }

    protected MuleEvent getEvent() throws Exception
    {
        return MuleTestUtils.getTestEvent(TEST_MESSAGE, MessageExchangePattern.REQUEST_RESPONSE, muleContext);
    }

    protected MuleEvent getEventWithId(String id) throws Exception
    {
        MuleEvent event = getEvent();
        event.setFlowVariable(ID_PROPERTY_NAME, id);
        return event;
    }

    public static class LetterMessageProcessor implements MessageProcessor
    {
        private String letter;

        public LetterMessageProcessor(String letter)
        {
            this.letter = letter;
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                event.getMessage().setPayload(letter);
                return event;
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }

}
