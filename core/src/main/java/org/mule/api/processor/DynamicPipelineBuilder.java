/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor;

import org.mule.api.MuleException;

import java.util.List;

/**
 * Updates a dynamic pipeline by ways of injecting message processor before and
 * after the static chain.
 * The injected message processors are executed before (pre) of after (post)
 * the ones defined in the flow in the specified order.
 * Also allows for resetting the dynamic chain.
 */
public interface DynamicPipelineBuilder
{

    /**
     * Helper builder for injecting message processors to be executed
     * before the ones specified in the flow.
     * After adding all required message processors {@link #resetAndUpdate()}
     * must be called.
     *
     * @param messageProcessors message processors to be executed before the ones specified in the flow
     * @return the pipeline injector builder instance
     */
    DynamicPipelineBuilder injectBefore(MessageProcessor... messageProcessors);

    /**
     * Helper builder for injecting message processors to be executed
     * before the ones specified in the flow.
     * After adding all required message processors {@link #resetAndUpdate()}
     * must be called.
     *
     * @param messageProcessors list of message processors to be executed before the ones specified in the flow
     * @return the pipeline injector builder instance
     */
    DynamicPipelineBuilder injectBefore(List<MessageProcessor> messageProcessors);

    /**
     * Helper builder for injecting message processors to be executed
     * after the ones specified in the flow.
     * After adding all required message processors {@link #resetAndUpdate()}
     * must be called.
     *
     * @param messageProcessors message processors to be executed after the ones specified in the flow
     * @return the pipeline injector builder instance
     */
    DynamicPipelineBuilder injectAfter(MessageProcessor... messageProcessors);

    /**
     * Helper builder for injecting message processors to be executed
     * after the ones specified in the flow.
     * After adding all required message processors {@link #resetAndUpdate()}
     * must be called.
     *
     * @param messageProcessors list of message processors to be executed after the ones specified in the flow
     * @return the pipeline injector builder instance
     */
    DynamicPipelineBuilder injectAfter(List<MessageProcessor> messageProcessors);

    /**
     * Injects the message processors added with {@link #injectBefore(org.mule.api.processor.MessageProcessor...)}
     * and {@link #injectAfter(org.mule.api.processor.MessageProcessor...)}
     * If none were added the effect is the same as calling {@link #reset()}
     *
     * @return pipeline ID for future updates
     * @throws org.mule.api.MuleException if the update fails
     */
    String resetAndUpdate() throws MuleException;

    /**
     * Removes and disposes all injected message processors.
     *
     * @return pipeline ID for future updates
     * @throws org.mule.api.MuleException if the update fails
     */
    String reset() throws MuleException;

}
