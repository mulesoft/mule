/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.construct.ThreadPerProcessorProcessingStrategy;
import org.mule.construct.AsynchronousProcessingStrategy;
import org.mule.construct.Flow;
import org.mule.construct.QueuedThreadPerProcessorProcessingStrategy;
import org.mule.construct.QueuedAsynchronousProcessingStrategy;
import org.mule.construct.SynchronousProcessingStrategy;

import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class FlowDefinitionParser extends OrphanDefinitionParser
{
    private static String PROCESSING_STRATEGY_ATTRIBUTE_NAME = "processingStrategy";

    private static String SYNC_PROCESSING_STRATEGY = "synchronous";
    private static String ASYNC_PROCESSING_STRATEGY = "asynchronous";
    private static String QUEUED_ASYNC_PROCESSING_STRATEGY = "queued-asynchronous";
    private static String THREAD_PER_PROCESSOR_PROCESSING_STRATEGY = "thread-per-processor";
    private static String QUEUED_THREAD_PER_PROCESSOR_PROCESSING_STRATEGY = "queued-thread-per-processor";
    
    

    public FlowDefinitionParser()
    {
        super(Flow.class, true);
        addIgnored("abstract");
        addIgnored("name");
        addIgnored("processingStrategy");
    }

    @java.lang.Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        builder.addConstructorArgValue(element.getAttribute(ATTRIBUTE_NAME));
        builder.addConstructorArgReference(MuleProperties.OBJECT_MULE_CONTEXT);
        String processingStrategy = element.getAttribute(PROCESSING_STRATEGY_ATTRIBUTE_NAME);
        if (SYNC_PROCESSING_STRATEGY.equals(processingStrategy))
        {
            builder.addPropertyValue(PROCESSING_STRATEGY_ATTRIBUTE_NAME, new SynchronousProcessingStrategy());
        }
        else if (ASYNC_PROCESSING_STRATEGY.equals(processingStrategy))
        {
            builder.addPropertyValue(PROCESSING_STRATEGY_ATTRIBUTE_NAME, new AsynchronousProcessingStrategy());
        }
        else if (QUEUED_ASYNC_PROCESSING_STRATEGY.equals(processingStrategy))
        {
            builder.addPropertyValue(PROCESSING_STRATEGY_ATTRIBUTE_NAME, new QueuedAsynchronousProcessingStrategy());
        }
        else if (THREAD_PER_PROCESSOR_PROCESSING_STRATEGY.equals(processingStrategy))
        {
            builder.addPropertyValue(PROCESSING_STRATEGY_ATTRIBUTE_NAME, new ThreadPerProcessorProcessingStrategy());
        }
        else if (QUEUED_THREAD_PER_PROCESSOR_PROCESSING_STRATEGY.equals(processingStrategy))
        {
            builder.addPropertyValue(PROCESSING_STRATEGY_ATTRIBUTE_NAME, new QueuedThreadPerProcessorProcessingStrategy());
        }
        else if (null != processingStrategy)
        {
            builder.addPropertyValue(PROCESSING_STRATEGY_ATTRIBUTE_NAME, new RuntimeBeanNameReference(
                processingStrategy));
        }

        super.doParse(element, parserContext, builder);
    }
}
