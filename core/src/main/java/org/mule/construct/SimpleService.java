/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.processor.FlowConstructStatisticsMessageObserver;
import org.mule.interceptor.LoggingInterceptor;
import org.mule.processor.builder.InterceptingChainMessageProcessorBuilder;

/**
 * In-out SOA-style simple service, with no outbound router. Always fully
 * synchronous.
 */
public class SimpleService extends AbstractFlowConstruct
{
    private final Component component;

    public SimpleService(String name,
                         MuleContext muleContext,
                         MessageSource messageSource,
                         Component component) throws MuleException
    {
        super(name, muleContext);

        if (messageSource == null)
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("messageSource can't be null on: " + this.toString()));
        }

        if (component == null)
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("component can't be null on: " + this.toString()));
        }

        this.messageSource = messageSource;
        this.component = component;
    }

    @Override
    protected void configureMessageProcessors(InterceptingChainMessageProcessorBuilder builder)
    {
        builder.chain(new LoggingInterceptor());
        builder.chain(new FlowConstructStatisticsMessageObserver());
        builder.chain(component);
    }

    @Override
    protected void validateConstruct() throws FlowConstructInvalidException
    {
        super.validateConstruct();

        if ((messageSource instanceof InboundEndpoint)
            && (!((InboundEndpoint) messageSource).getExchangePattern().equals(
                MessageExchangePattern.REQUEST_RESPONSE)))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("SimpleService only works with a request-response inbound endpoint."),
                this);
        }
    }

    public Component getComponent()
    {
        return component;
    }

}
