/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor;

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
     * Provide access to a {@link DynamicPipelineBuilder} that allows modifying
     * the dynamic pipeline injecting message processors and resetting the pipeline
     *
     * @param id dynamic pipeline ID
     * @return a DynamicPipelineBuilder that allows modifying the dynamic pipeline
     * @throws DynamicPipelineException if the pipeline ID is not valid
     */
    DynamicPipelineBuilder dynamicPipeline(String id) throws DynamicPipelineException;

}
