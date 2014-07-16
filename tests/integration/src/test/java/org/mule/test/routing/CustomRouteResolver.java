/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.DynamicRouteResolver;

import java.util.ArrayList;
import java.util.List;

public class CustomRouteResolver implements DynamicRouteResolver
{

    static List<MessageProcessor> routes = new ArrayList<MessageProcessor>();

    @Override
    public List<MessageProcessor> resolveRoutes(MuleEvent event)
    {
        return routes;
    }

    public static class AddLetterMessageProcessor implements MessageProcessor
    {

        private String letter;

        public AddLetterMessageProcessor(String letter)
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

    public static class FailingMessageProcessor implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new DefaultMuleException(CoreMessages.createStaticMessage(""));
        }
    }

    public static class AddLetterTHenFailsMessageProcessor implements MessageProcessor
    {

        private String letter;

        public AddLetterTHenFailsMessageProcessor(String letter)
        {
            this.letter = letter;
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                event.getMessage().setPayload(event.getMessage().getPayloadAsString() + letter);
            }
            catch (Exception e)
            {
            }
            throw new DefaultMuleException(CoreMessages.createStaticMessage(""));
        }
    }
}
