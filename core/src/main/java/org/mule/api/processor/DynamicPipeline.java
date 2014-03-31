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
 * Adds to a pipeline the ability to dynamically inject a sequence
 * of message processors after initialization.
 *
 * The injected message processors are executed before (pre) of after (post)
 * the ones defined in the flow in the specified order.
 *
 */
public interface DynamicPipeline
{

    /**
     * Updates the pipeline injecting the lists of preMessageProcessors and postMessageProcessors.
     * In case there was a previous dynamic chain, the message processors are disposed.
     *
     * @param id dynamic pipeline ID
     * @param preMessageProcessors message processors to be executed before the ones specified in the flow
     * @param postMessageProcessors message processors to be executed after the ones specified in the flow
     * @return pipeline ID for future updates
     * @throws MuleException if the update fails
     */
    String resetAndUpdatePipeline(String id, List<MessageProcessor> preMessageProcessors, List<MessageProcessor> postMessageProcessors) throws MuleException;

    /**
     * Removes and disposes all injected message processors.
     *
     * @param id dynamic pipeline ID
     * @return pipeline ID for future updates
     * @throws MuleException if the update fails
     */
    String resetPipeline(String id) throws MuleException;

    /**
     * Helper builder for injecting message processors to be executed
     * before the ones specified in the flow.
     * After adding all required message processors {@link #resetAndUpdatePipeline(String)}
     * must be called.
     *
     * @param messageProcessors message processors to be executed before the ones specified in the flow
     * @return the pipeline injector builder instance
     */
    DynamicPipeline injectBefore(MessageProcessor... messageProcessors);

    /**
     * Helper builder for injecting message processors to be executed
     * after the ones specified in the flow.
     * After adding all required message processors {@link #resetAndUpdatePipeline(String)}
     * must be called.
     *
     * @param messageProcessors message processors to be executed after the ones specified in the flow
     * @return the pipeline injector builder instance
     */
    DynamicPipeline injectAfter(MessageProcessor... messageProcessors);

    /**
     * Injects the message processors added with {@link #injectBefore(MessageProcessor...)} and {@link #injectAfter(MessageProcessor...)}
     * If none were added the effect is the same as calling {@link #resetPipeline(String)}
     *
     * @param id dynamic pipeline ID
     * @return pipeline ID for future updates
     * @throws MuleException if the update fails
     */
    String resetAndUpdatePipeline(String id) throws MuleException;

}
