/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.util;

import org.mule.api.processor.ProcessingStrategy;
import org.mule.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.processor.strategy.QueuedAsynchronousProcessingStrategy;
import org.mule.processor.strategy.QueuedThreadPerProcessorProcessingStrategy;
import org.mule.processor.strategy.SynchronousProcessingStrategy;
import org.mule.processor.strategy.ThreadPerProcessorProcessingStrategy;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

public class ProcessingStrategyUtils
{

    private static String PROCESSING_STRATEGY_ATTRIBUTE_NAME = "processingStrategy";

    public static String DEFAULT_PROCESSING_STRATEGY = "default";
    public static String SYNC_PROCESSING_STRATEGY = "synchronous";
    public static String NON_BLOCKING_PROCESSING_STRATEGY = "non-blocking";
    public static String ASYNC_PROCESSING_STRATEGY = "asynchronous";
    public static String QUEUED_ASYNC_PROCESSING_STRATEGY = "queued-asynchronous";
    public static String THREAD_PER_PROCESSOR_PROCESSING_STRATEGY = "thread-per-processor";
    public static String QUEUED_THREAD_PER_PROCESSOR_PROCESSING_STRATEGY = "queued-thread-per-processor";

    public static void configureProcessingStrategy(Element element,
                                                   BeanDefinitionBuilder builder,
                                                   String defaultStrategy)
    {
        String processingStrategyName = element.getAttribute(PROCESSING_STRATEGY_ATTRIBUTE_NAME);
        ProcessingStrategy processingStrategy = parseProcessingStrategy(processingStrategyName);
        if (processingStrategy != null)
        {
            builder.addPropertyValue(PROCESSING_STRATEGY_ATTRIBUTE_NAME, processingStrategy);
        }
        else if (!StringUtils.isBlank(processingStrategyName))
        {
            builder.addPropertyValue(PROCESSING_STRATEGY_ATTRIBUTE_NAME, new RuntimeBeanReference(processingStrategyName));

        }
    }

    public static ProcessingStrategy parseProcessingStrategy(String processingStrategy)
    {

        if (DEFAULT_PROCESSING_STRATEGY.equals(processingStrategy))
        {
            return new DefaultFlowProcessingStrategy();
        }
        else if (SYNC_PROCESSING_STRATEGY.equals(processingStrategy))
        {
            return new SynchronousProcessingStrategy();
        }
        else if (NON_BLOCKING_PROCESSING_STRATEGY.equals(processingStrategy))
        {
            return new NonBlockingProcessingStrategy();
        }
        else if (ASYNC_PROCESSING_STRATEGY.equals(processingStrategy))
        {
            return new AsynchronousProcessingStrategy();
        }
        else if (QUEUED_ASYNC_PROCESSING_STRATEGY.equals(processingStrategy))
        {
            return new QueuedAsynchronousProcessingStrategy();
        }
        else if (THREAD_PER_PROCESSOR_PROCESSING_STRATEGY.equals(processingStrategy))
        {
            return new ThreadPerProcessorProcessingStrategy();
        }
        else if (QUEUED_THREAD_PER_PROCESSOR_PROCESSING_STRATEGY.equals(processingStrategy))
        {
            return new QueuedThreadPerProcessorProcessingStrategy();
        }

        return null;
    }

}
