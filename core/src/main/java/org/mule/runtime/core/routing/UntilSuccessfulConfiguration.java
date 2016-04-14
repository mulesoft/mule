/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.store.ListableObjectStore;
import org.mule.routing.filters.ExpressionFilter;

import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Configuration required for UntilSuccessful router processing strategy.
 */
public interface UntilSuccessfulConfiguration
{

    /**
     * @return threading profile to executes message processing inside until successful. Always returns a not null value.
     */
    ThreadingProfile getThreadingProfile();

    /**
     * @param threadPrefix the prefix for the name of the threads of this executor's pool.
     * @return an executor responsible for calling the retries for this processing strategy.
     */
    ScheduledThreadPoolExecutor createScheduledRetriesPool(final String threadPrefix);

    /**
     * @return an ObjectStore to store until successful internal data. Always returns a not null value.
     */
    ListableObjectStore<MuleEvent> getObjectStore();

    /**
     * @return ExpressionFilter to determine if the message was processed successfully or not. Always returns a not null value.
     */
    ExpressionFilter getFailureExpressionFilter();

    /**
     * @return the route to which the message should be routed to. Always returns a not null value.
     */
    MessageProcessor getRoute();

    /**
     * @return the MuleContext within the until-successful router was defined. Always returns a not null value.
     */
    MuleContext getMuleContext();

    /**
     * @return the FlowConstruct within the until-successful router was defined. Always returns a not null value.
     */
    FlowConstruct getFlowConstruct();

    /**
     * @return the expression that will define the returned payload after the until successful route execution.
     */
    String getAckExpression();

    /**
     * @return the number of milliseconds between retries. Default value is 60000.
     */
    long getMillisBetweenRetries();

    /**
     * @return the number of retries to process the route when failing. Default value is 5.
     */
    int getMaxRetries();

    /**
     * @return the route to which the message must be sent if the processing fails.
     */
    MessageProcessor getDlqMP();

    /**
     * @return the until sucessful router instance.
     */
    MessageProcessor getRouter();
}
