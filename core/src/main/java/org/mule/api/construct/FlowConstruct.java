/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.construct;

import org.mule.api.MuleContext;
import org.mule.api.NamedObject;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.LifecycleStateEnabled;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.source.MessageSource;
import org.mule.management.stats.FlowConstructStatistics;

/**
 * A uniquely identified {@link FlowConstruct} that once implemented and configured defines a construct
 * through which messages are processed using {@link MessageSource} and {@link MessageProcessor} building
 * blocks.
 */
public interface FlowConstruct extends NamedObject, LifecycleStateEnabled
{

    /**
     * @return The exception listener that will be used to handle exceptions that may be thrown at different
     *         points during the message flow defined by this construct.
     */
    MessagingExceptionHandler getExceptionListener();

    /**
     * @return The statistics holder used by this flow construct to keep track of its activity.
     */
    FlowConstructStatistics getStatistics();

    /**
     * @return This implementation of {@link MessageInfoMapping} used to control how Important message
     *         information is pulled from the current message.
     */
    MessageInfoMapping getMessageInfoMapping();

    /**
     * @return This muleContext that this flow construct belongs to and runs in the context of.
     */
    MuleContext getMuleContext();

}
