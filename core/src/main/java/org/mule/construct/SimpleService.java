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

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.config.i18n.MessageFactory;
import org.mule.interceptor.LoggingInterceptor;
import org.mule.processor.builder.ChainMessageProcessorBuilder;

/**
 * In-out SOA-style simple service, with no outbound router. Always fully
 * synchronous.
 */
public class SimpleService extends AbstractFlowConstruct
{
    private Component component;

    public SimpleService(MuleContext muleContext, String name) throws MuleException
    {
        super(name, muleContext);
    }

    public void setComponent(Component component)
    {
        this.component = component;
    }

    @Override
    protected void configureMessageProcessors(ChainMessageProcessorBuilder builder)
    {
        builder.chain(new LoggingInterceptor());
        // TODO add builder.chain(statisticsInterceptingMessageProcess)
        builder.chain(component);
    }

    @Override
    protected void validateConstruct() throws FlowConstructInvalidException
    {
        super.validateConstruct();

        // TODO Ensure messageSource is a single InboundEndpoint (not composite)?
        // TODO Ensure InboundEndpoint messageSource has supported Exchange Pattern

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
    }

    public Component getComponent()
    {
        return component;
    }

}
