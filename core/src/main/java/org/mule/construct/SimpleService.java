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
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.source.MessageSource;
import org.mule.processor.builder.ChainMessageProcessorBuilder;

/**
 * In-out SOA-style simple service, with no outbound router. Always fully
 * synchronous.
 */
public class SimpleService extends AbstractFlowConstuct
{
    private final Component component;

    public SimpleService(MuleContext muleContext,
                         String name,
                         MessageSource messageSource,
                         Component component) throws MuleException
    {
        super(name, muleContext);
        this.messageSource = messageSource;
        this.component = component;
    }

    @Override
    protected void configureMessageProcessors(ChainMessageProcessorBuilder builder)
    {
        // builder.chain(loggingInterceptingMessageProcess)
        // builder.chain(statisticsInterceptingMessageProcess)
        builder.chain(component);
    }

    @Override
    protected void validateConstruct() throws InitialisationException
    {
        super.validateConstruct();
        // Ensure messageSource is a single InboundEndpoint (not composite)?
        // Ensure InboundEndpoint messageSource has supported Exchange Pattern

        // Should we create a new exception type for this maybe?
    }

    public Component getComponent()
    {
        return component;
    }

}
