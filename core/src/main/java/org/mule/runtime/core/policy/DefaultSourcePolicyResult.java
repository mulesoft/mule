/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.Event;

import java.util.Map;

/**
 * Default implementation of {@link SourcePolicyResult} that delegates the parameters generation directly to the source parameters processors.
 */
public class DefaultSourcePolicyResult implements SourcePolicyResult
{

    private final Event flowExecutionResult;
    private final MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor;

    public DefaultSourcePolicyResult(Event flowExecutionResult, MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor)
    {
        this.flowExecutionResult = flowExecutionResult;
        this.messageSourceResponseParametersProcessor = messageSourceResponseParametersProcessor;
    }

    @Override
    public Event getExecutionResult()
    {
        return flowExecutionResult;
    }

    @Override
    public Map<String, Object> getResponseParameters()
    {
        return messageSourceResponseParametersProcessor.getSuccessfulExecutionResponseParametersFunction().apply(flowExecutionResult);
    }

    @Override
    public Map<String, Object> getErrorResponseParameters(Event failureEvent)
    {
        return messageSourceResponseParametersProcessor.getFailedExecutionResponseParametersFunction().apply(failureEvent);
    }
}
